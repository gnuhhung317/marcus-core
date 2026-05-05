package io.marcus.application.usecase;

import io.marcus.application.dto.RemoveBotSubscriberRequest;
import io.marcus.application.dto.UpsertBotSubscriberRequest;
import io.marcus.application.dto.UpsertUserSessionRequest;
import io.marcus.domain.model.Bot;
import io.marcus.domain.model.Signal;
import io.marcus.domain.vo.SignalAction;
import io.marcus.domain.port.BotSubscriberRoutingPort;
import io.marcus.domain.port.SignalPublisherPort;
import io.marcus.domain.port.SignalServerDispatchPort;
import io.marcus.domain.port.UserSessionRoutingPort;
import io.marcus.domain.repository.BotRepository;
import io.marcus.domain.repository.SignalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E integration test for bot signal flow: 1. User subscribes to a bot 2. Bot
 * generates a signal 3. Signal is dispatched to subscribed user's websocket
 * channels
 *
 * Test data: - Bot: botId=bot_f25c000d5e90488c9c1c58fd5cd11843, Momentum Alpha,
 * BTC/USDT on Binance - User subscription:
 * wsToken=ws_a0f3e543ed5448a98b266e1b6ef1e96d, status=ACTIVE
 */
@DisplayName("Bot Signal E2E Flow Integration Test")
class BotSignalE2eFlowIntegrationTest {

    // Test data from provided credentials
    private static final String BOT_ID = "bot_f25c000d5e90488c9c1c58fd5cd11843";
    private static final String USER_ID = "user_e2e_test_001";
    private static final String WS_TOKEN = "ws_a0f3e543ed5448a98b266e1b6ef1e96d";
    private static final String WS_SESSION_ID = "session_" + WS_TOKEN;

    private UpsertBotSubscriberUseCase upsertBotSubscriberUseCase;
    private RemoveBotSubscriberUseCase removeBotSubscriberUseCase;
    private UpsertUserSessionUseCase upsertUserSessionUseCase;
    private CaptureSignalUseCase captureSignalUseCase;

    private InMemorySignalRepository inMemorySignalRepository;
    private InMemoryBotRepository inMemoryBotRepository;
    private InMemorySignalPublisherPort inMemorySignalPublisherPort;
    private InMemorySignalServerDispatchPort inMemorySignalServerDispatchPort;
    private InMemoryBotSubscriberRoutingAdapter botSubscriberRoutingPort;
    private InMemoryUserSessionRoutingAdapter userSessionRoutingPort;

    @BeforeEach
    void setUp() {
        botSubscriberRoutingPort = new InMemoryBotSubscriberRoutingAdapter();
        userSessionRoutingPort = new InMemoryUserSessionRoutingAdapter();
        inMemorySignalRepository = new InMemorySignalRepository();
        inMemoryBotRepository = new InMemoryBotRepository();
        inMemorySignalPublisherPort = new InMemorySignalPublisherPort();
        inMemorySignalServerDispatchPort = new InMemorySignalServerDispatchPort();

        upsertBotSubscriberUseCase = new UpsertBotSubscriberUseCase(botSubscriberRoutingPort);
        removeBotSubscriberUseCase = new RemoveBotSubscriberUseCase(botSubscriberRoutingPort);
        upsertUserSessionUseCase = new UpsertUserSessionUseCase(userSessionRoutingPort);

        ResolveBotRoutingTargetsUseCase resolveBotRoutingTargetsUseCase
                = new ResolveBotRoutingTargetsUseCase(botSubscriberRoutingPort, userSessionRoutingPort);
        captureSignalUseCase = new CaptureSignalUseCase(
                inMemorySignalRepository,
                inMemoryBotRepository,
                resolveBotRoutingTargetsUseCase,
                inMemorySignalPublisherPort,
                inMemorySignalServerDispatchPort
        );
        
        // Register test bot
        inMemoryBotRepository.registerBot(BOT_ID);
    }

    @Test
    @DisplayName("E2E: User subscribes to bot and receives dispatched signal via websocket")
    void shouldDispatchSignalToBotSubscriberViaWebsocket() {
        // Step 1: User subscribes to bot (registers bot subscriber routing entry)
        upsertBotSubscriberUseCase.execute(new UpsertBotSubscriberRequest(BOT_ID, USER_ID));

        // Step 2: User connects websocket session with subscription token
        upsertUserSessionUseCase.execute(new UpsertUserSessionRequest(USER_ID, WS_SESSION_ID, WS_TOKEN));

        // Step 3: Bot generates and sends signal to backend
        Signal signal = Signal.builder()
                .signalId("signal_momentum_001")
                .botId(BOT_ID)
                .symbol("BTC/USDT")
                .action(SignalAction.OPEN_LONG)
                .build();

        // Step 4: Backend captures signal and dispatches to subscribed users' websockets
        captureSignalUseCase.execute(signal);

        // Assertions: Verify signal was published
        assertThat(inMemorySignalRepository.savedSignals)
                .hasSize(1)
                .containsExactly(signal);

        // Assertions: Verify signal was dispatched to subscribed websocket channel
        assertThat(inMemorySignalServerDispatchPort.dispatches)
                .hasSize(1)
                .extracting(DispatchRecord::signalId)
                .containsExactly("signal_momentum_001");

        assertThat(inMemorySignalServerDispatchPort.dispatches.get(0).serverIds())
                .containsExactly(WS_TOKEN);
    }

    @Test
    @DisplayName("E2E: Multiple bot signals are dispatched to same subscriber")
    void shouldDispatchMultipleSignalsToSameBotSubscriber() {
        // Subscribe user to bot
        upsertBotSubscriberUseCase.execute(new UpsertBotSubscriberRequest(BOT_ID, USER_ID));
        upsertUserSessionUseCase.execute(new UpsertUserSessionRequest(USER_ID, WS_SESSION_ID, WS_TOKEN));

        // Send first signal
        Signal signal1 = Signal.builder()
                .signalId("signal_momentum_001")
                .botId(BOT_ID)
                .symbol("BTC/USDT")
                .action(SignalAction.OPEN_LONG)
                .build();
        captureSignalUseCase.execute(signal1);

        // Send second signal
        Signal signal2 = Signal.builder()
                .signalId("signal_momentum_002")
                .botId(BOT_ID)
                .symbol("BTC/USDT")
                .action(SignalAction.CLOSE_LONG)
                .build();
        captureSignalUseCase.execute(signal2);

        // Verify both signals were published and dispatched
        assertThat(inMemorySignalRepository.savedSignals)
                .hasSize(2)
                .containsExactly(signal1, signal2);

        assertThat(inMemorySignalServerDispatchPort.dispatches)
                .hasSize(2)
                .extracting(DispatchRecord::signalId)
                .containsExactlyInAnyOrder("signal_momentum_001", "signal_momentum_002");
    }

    @Test
    @DisplayName("E2E: Unsubscribed user does not receive bot signals")
    void shouldNotDispatchSignalAfterUnsubscribe() {
        // Subscribe and send first signal
        upsertBotSubscriberUseCase.execute(new UpsertBotSubscriberRequest(BOT_ID, USER_ID));
        upsertUserSessionUseCase.execute(new UpsertUserSessionRequest(USER_ID, WS_SESSION_ID, WS_TOKEN));

        Signal firstSignal = Signal.builder()
                .signalId("signal_momentum_001")
                .botId(BOT_ID)
                .symbol("BTC/USDT")
                .action(SignalAction.OPEN_LONG)
                .build();
        captureSignalUseCase.execute(firstSignal);

        // Unsubscribe
        removeBotSubscriberUseCase.execute(new RemoveBotSubscriberRequest(BOT_ID, USER_ID));

        // Send second signal after unsubscribe
        Signal secondSignal = Signal.builder()
                .signalId("signal_momentum_002")
                .botId(BOT_ID)
                .symbol("BTC/USDT")
                .action(SignalAction.CLOSE_LONG)
                .build();
        captureSignalUseCase.execute(secondSignal);

        // Verify only first signal was dispatched
        assertThat(inMemorySignalServerDispatchPort.dispatches)
                .hasSize(1)
                .extracting(DispatchRecord::signalId)
                .containsExactly("signal_momentum_001");
    }

    @Test
    @DisplayName("E2E: Only subscribed bot receives signals (isolation)")
    void shouldDispatchOnlyToSubscribedBotSubscribers() {
        String otherBotId = "bot_other_strategy_001";
        String otherUserId = "user_other_001";

        // Register the other bot (since it doesn't exist in setUp)
        inMemoryBotRepository.registerBot(otherBotId);

        // User subscribes to BOT_ID
        upsertBotSubscriberUseCase.execute(new UpsertBotSubscriberRequest(BOT_ID, USER_ID));
        upsertUserSessionUseCase.execute(new UpsertUserSessionRequest(USER_ID, WS_SESSION_ID, WS_TOKEN));

        // Other user subscribes to different bot (but same websocket token)
        upsertBotSubscriberUseCase.execute(new UpsertBotSubscriberRequest(otherBotId, otherUserId));
        upsertUserSessionUseCase.execute(new UpsertUserSessionRequest(otherUserId, "session_other", "ws_other_token"));

        // Signal from BOT_ID is captured
        Signal signal = Signal.builder()
                .signalId("signal_momentum_001")
                .botId(BOT_ID)
                .symbol("BTC/USDT")
                .action(SignalAction.OPEN_LONG)
                .build();
        captureSignalUseCase.execute(signal);

        // Verify signal dispatched only to USER_ID's websocket
        assertThat(inMemorySignalServerDispatchPort.dispatches)
                .hasSize(1)
                .extracting(DispatchRecord::serverIds)
                .containsExactly(new LinkedHashSet<>(List.of(WS_TOKEN)));
    }

    // ============ In-Memory Adapters (reuse pattern from CaptureSignalRoutingFlowIntegrationTest) ============
    private static final class InMemoryBotRepository implements BotRepository {
        private final Set<String> botIds = new HashSet<>();

        public void registerBot(String botId) {
            botIds.add(botId);
        }

        @Override
        public Bot save(Bot bot) {
            return bot;
        }

        @Override
        public Optional<Bot> findByBotId(String botId) {
            return botIds.contains(botId) ? Optional.of(new Bot()) : Optional.empty();
        }

        @Override
        public List<Bot> findAllActive() {
            return new ArrayList<>();
        }

        @Override
        public List<Bot> findAllByDeveloperId(String developerId) {
            return new ArrayList<>();
        }

        @Override
        public Optional<String> findSecretByBotId(String botId) {
            return Optional.empty();
        }

        @Override
        public Optional<String> findSecretByApiKey(String apiKey) {
            return Optional.empty();
        }
    }

    private static final class InMemoryBotSubscriberRoutingAdapter implements BotSubscriberRoutingPort {

        private final Map<String, Set<String>> subscribersByBot = new HashMap<>();

        @Override
        public void upsertSubscriber(String botId, String userId) {
            subscribersByBot.computeIfAbsent(botId, ignored -> new LinkedHashSet<>()).add(userId);
        }

        @Override
        public void removeSubscriber(String botId, String userId) {
            Set<String> subscribers = subscribersByBot.get(botId);
            if (subscribers == null) {
                return;
            }
            subscribers.remove(userId);
        }

        @Override
        public Set<String> findActiveSubscriberUserIdsByBotId(String botId) {
            return new LinkedHashSet<>(subscribersByBot.getOrDefault(botId, Set.of()));
        }
    }

    private static final class InMemoryUserSessionRoutingAdapter implements UserSessionRoutingPort {

        private final Map<String, Set<String>> sessionsByUser = new HashMap<>();
        private final Map<String, String> serverBySession = new HashMap<>();

        @Override
        public void upsertSession(String userId, String sessionId, String serverId) {
            sessionsByUser.computeIfAbsent(userId, ignored -> new LinkedHashSet<>()).add(sessionId);
            serverBySession.put(sessionId, serverId);
        }

        @Override
        public void removeSession(String userId, String sessionId) {
            Set<String> sessions = sessionsByUser.get(userId);
            if (sessions != null) {
                sessions.remove(sessionId);
            }
            serverBySession.remove(sessionId);
        }

        @Override
        public Set<String> findServerIdsByUserIds(Set<String> userIds) {
            Set<String> serverIds = new HashSet<>();
            for (String userId : userIds) {
                Set<String> sessionIds = sessionsByUser.getOrDefault(userId, Set.of());
                for (String sessionId : sessionIds) {
                    String serverId = serverBySession.get(sessionId);
                    if (serverId != null && !serverId.isBlank()) {
                        serverIds.add(serverId);
                    }
                }
            }
            return serverIds;
        }
    }

    private static final class InMemorySignalRepository implements SignalRepository {

        private final List<Signal> savedSignals = new ArrayList<>();

        @Override
        public void save(Signal signal) {
            savedSignals.add(signal);
        }

        @Override
        public boolean existsBySignalId(String signalId) {
            return savedSignals.stream()
                    .anyMatch(signal -> signal.getSignalId().equals(signalId));
        }
    }

    private static final class InMemorySignalPublisherPort implements SignalPublisherPort {

        private final List<Signal> publishedSignals = new ArrayList<>();

        @Override
        public void publish(Signal signal) {
            publishedSignals.add(signal);
        }
    }

    private static final class InMemorySignalServerDispatchPort implements SignalServerDispatchPort {

        private final List<DispatchRecord> dispatches = new ArrayList<>();

        @Override
        public void dispatchToServers(Signal signal, Set<String> serverIds) {
            dispatches.add(new DispatchRecord(signal.getSignalId(), new LinkedHashSet<>(serverIds)));
        }
    }

    private record DispatchRecord(String signalId, Set<String> serverIds) {

    }
}
