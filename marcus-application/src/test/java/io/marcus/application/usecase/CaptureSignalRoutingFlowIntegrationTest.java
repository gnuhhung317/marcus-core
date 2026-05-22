package io.marcus.application.usecase;

import io.marcus.application.dto.RemoveBotSubscriberRequest;
import io.marcus.application.dto.RemoveUserSessionRequest;
import io.marcus.application.dto.UpsertBotSubscriberRequest;
import io.marcus.application.dto.UpsertUserSessionRequest;
import io.marcus.domain.model.Bot;
import io.marcus.domain.model.Signal;
import io.marcus.domain.vo.SignalStatus;
import io.marcus.domain.port.BotSubscriberRoutingPort;
import io.marcus.domain.port.SignalPublisherPort;
import io.marcus.domain.port.SignalServerDispatchPort;
import io.marcus.domain.port.UserSessionRoutingPort;
import io.marcus.domain.repository.BotRepository;
import io.marcus.domain.repository.SignalRepository;
import org.junit.jupiter.api.BeforeEach;
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

class CaptureSignalRoutingFlowIntegrationTest {

    private UpsertBotSubscriberUseCase upsertBotSubscriberUseCase;
    private RemoveBotSubscriberUseCase removeBotSubscriberUseCase;
    private UpsertUserSessionUseCase upsertUserSessionUseCase;
    private RemoveUserSessionUseCase removeUserSessionUseCase;
    private CaptureSignalUseCase captureSignalUseCase;

    private InMemorySignalRepository inMemorySignalRepository;
    private InMemoryBotRepository inMemoryBotRepository;
    private InMemorySignalPublisherPort inMemorySignalPublisherPort;
    private InMemorySignalServerDispatchPort inMemorySignalServerDispatchPort;

    @BeforeEach
    void setUp() {
        InMemoryBotSubscriberRoutingAdapter botSubscriberRoutingPort = new InMemoryBotSubscriberRoutingAdapter();
        InMemoryUserSessionRoutingAdapter userSessionRoutingPort = new InMemoryUserSessionRoutingAdapter();
        inMemorySignalRepository = new InMemorySignalRepository();
        inMemoryBotRepository = new InMemoryBotRepository();
        inMemorySignalPublisherPort = new InMemorySignalPublisherPort();
        inMemorySignalServerDispatchPort = new InMemorySignalServerDispatchPort();

        upsertBotSubscriberUseCase = new UpsertBotSubscriberUseCase(botSubscriberRoutingPort);
        removeBotSubscriberUseCase = new RemoveBotSubscriberUseCase(botSubscriberRoutingPort);
        upsertUserSessionUseCase = new UpsertUserSessionUseCase(userSessionRoutingPort);
        removeUserSessionUseCase = new RemoveUserSessionUseCase(userSessionRoutingPort);

        ResolveBotRoutingTargetsUseCase resolveBotRoutingTargetsUseCase = new ResolveBotRoutingTargetsUseCase(
                botSubscriberRoutingPort, userSessionRoutingPort);
        captureSignalUseCase = new CaptureSignalUseCase(
                inMemorySignalRepository,
                inMemoryBotRepository,
                resolveBotRoutingTargetsUseCase,
                inMemorySignalPublisherPort,
                inMemorySignalServerDispatchPort);
    }

    @Test
    void shouldDispatchToResolvedTargetsWhenSubscriberSessionChainExists() {
        // Register bot as existing
        inMemoryBotRepository.registerBot("bot-1");

        upsertBotSubscriberUseCase.execute(new UpsertBotSubscriberRequest("bot-1", "user-1"));
        upsertBotSubscriberUseCase.execute(new UpsertBotSubscriberRequest("bot-1", "user-2"));

        upsertUserSessionUseCase.execute(new UpsertUserSessionRequest("user-1", "session-1", "ws-a"));
        upsertUserSessionUseCase.execute(new UpsertUserSessionRequest("user-2", "session-2", "ws-b"));
        upsertUserSessionUseCase.execute(new UpsertUserSessionRequest("user-2", "session-3", "ws-b"));

        io.marcus.application.dto.CaptureSignalRequest signalRequest = new io.marcus.application.dto.CaptureSignalRequest(
                "signal-1",
                "bot-1",
                "BTCUSDT",
                io.marcus.domain.vo.SignalAction.OPEN_LONG,
                new java.math.BigDecimal("50000"),
                null,
                null,
                null,
                null,
                null,
                null
        );
        captureSignalUseCase.execute(signalRequest);

        assertThat(inMemorySignalRepository.savedSignals)
                .hasSize(1);
        assertThat(inMemorySignalRepository.savedSignals.get(0).getSignalId())
                .isEqualTo("signal-1");
        assertThat(inMemorySignalServerDispatchPort.dispatches)
                .hasSize(1);
        assertThat(inMemorySignalServerDispatchPort.dispatches.get(0).serverIds())
                .containsExactlyInAnyOrder("ws-a", "ws-b");
    }

    @Test
    void shouldStopDispatchingWhenSubscriberAndSessionAreRemoved() {
        // Register bot
        inMemoryBotRepository.registerBot("bot-1");

        upsertBotSubscriberUseCase.execute(new UpsertBotSubscriberRequest("bot-1", "user-1"));
        upsertBotSubscriberUseCase.execute(new UpsertBotSubscriberRequest("bot-1", "user-2"));

        upsertUserSessionUseCase.execute(new UpsertUserSessionRequest("user-1", "session-1", "ws-a"));
        upsertUserSessionUseCase.execute(new UpsertUserSessionRequest("user-2", "session-2", "ws-b"));

        io.marcus.application.dto.CaptureSignalRequest firstSignal = new io.marcus.application.dto.CaptureSignalRequest(
                "signal-1",
                "bot-1",
                "BTCUSDT",
                io.marcus.domain.vo.SignalAction.OPEN_LONG,
                new java.math.BigDecimal("50000"),
                null,
                null,
                null,
                null,
                null,
                null
        );
        captureSignalUseCase.execute(firstSignal);

        removeBotSubscriberUseCase.execute(new RemoveBotSubscriberRequest("bot-1", "user-2"));
        removeUserSessionUseCase.execute(new RemoveUserSessionRequest("user-1", "session-1"));

        io.marcus.application.dto.CaptureSignalRequest secondSignal = new io.marcus.application.dto.CaptureSignalRequest(
                "signal-2",
                "bot-1",
                "BTCUSDT",
                io.marcus.domain.vo.SignalAction.OPEN_LONG,
                new java.math.BigDecimal("50000"),
                null,
                null,
                null,
                null,
                null,
                null
        );
        captureSignalUseCase.execute(secondSignal);

        assertThat(inMemorySignalRepository.savedSignals)
                .hasSize(2);
        assertThat(inMemorySignalRepository.savedSignals)
                .extracting(Signal::getSignalId)
                .containsExactly("signal-1", "signal-2");
        assertThat(inMemorySignalServerDispatchPort.dispatches)
                .hasSize(1);
        assertThat(inMemorySignalServerDispatchPort.dispatches.get(0).signalId())
                .isEqualTo("signal-1");
    }

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

        @Override
        public long countActive() {
            return 0;
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

        @Override
        public void updateStatus(String signalId, SignalStatus status) {

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
