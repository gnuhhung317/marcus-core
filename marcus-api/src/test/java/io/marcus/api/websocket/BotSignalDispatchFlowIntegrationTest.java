package io.marcus.api.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.api.websocket.executor.ExecutorEventEventHandler;
import io.marcus.application.dto.UpsertUserSessionRequest;
import io.marcus.application.usecase.CaptureSignalUseCase;
import io.marcus.application.usecase.ResolveBotRoutingTargetsUseCase;
import io.marcus.application.usecase.SubscribeBotUseCase;
import io.marcus.application.usecase.UpsertUserSessionUseCase;
import io.marcus.domain.model.Bot;
import io.marcus.domain.model.Signal;
import io.marcus.domain.model.UserSubscription;
import io.marcus.domain.port.BotSubscriberRoutingPort;
import io.marcus.domain.port.SignalPublisherPort;
import io.marcus.domain.port.SignalServerDispatchPort;
import io.marcus.domain.port.UserSessionRoutingPort;
import io.marcus.domain.port.UserSubscriptionPersistencePort;
import io.marcus.domain.repository.BotRepository;
import io.marcus.domain.repository.UserRepository;
import io.marcus.domain.service.IdentityService;
import io.marcus.domain.vo.BotStatus;
import io.marcus.domain.vo.Role;
import io.marcus.domain.vo.SignalAction;
import io.marcus.domain.vo.SignalStatus;
import io.marcus.domain.vo.SubscriptionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BotSignalDispatchFlowIntegrationTest {

    @Test
    void shouldSubscribeBotAndDispatchSignalToConnectedExecutorClient() throws Exception {
        String botId = "bot_f25c000d5e90488c9c1c58fd5cd11843";
        String userId = "usr_1";
        String wsToken = "ws_a0f3e543ed5448a98b266e1b6ef1e96d";
        String sessionId = "session-1";
        String serverId = "server-1";

        Bot bot = Bot.builder()
                .botId(botId)
                .name("Momentum Alpha")
                .description("Momentum bot for BTC/USDT")
                .status(BotStatus.ACTIVE)
                .apiKey("ak_2839eb9cdfad4b9abc4ee4e3865b2635")
                .secretKey("sk_b02a6a6baaf34ae885216de07256a7c1")
                .tradingPair("BTC/USDT")
                .exchangeId("binance")
                .build();

        InMemoryBotRepository botRepository = new InMemoryBotRepository();
        botRepository.save(bot);
        InMemoryUserRepository userRepository = new InMemoryUserRepository(userId);
        InMemoryUserSubscriptionPersistencePort subscriptionPersistencePort
                = new InMemoryUserSubscriptionPersistencePort(Map.of(userId, wsToken));
        InMemoryBotSubscriberRoutingPort botSubscriberRoutingPort = new InMemoryBotSubscriberRoutingPort();
        InMemoryUserSessionRoutingPort userSessionRoutingPort = new InMemoryUserSessionRoutingPort();

        IdentityService identityService = () -> Optional.of(userId);

        SubscribeBotUseCase subscribeBotUseCase = new SubscribeBotUseCase(
                identityService,
                userRepository,
                botRepository,
                subscriptionPersistencePort,
                botSubscriberRoutingPort
        );

        var subscribeResult = subscribeBotUseCase.execute(botId);
        assertThat(subscribeResult.botId()).isEqualTo(botId);
        assertThat(subscribeResult.wsToken()).isEqualTo(wsToken);
        assertThat(subscribeResult.status()).isEqualTo(SubscriptionStatus.ACTIVE.name());

        UpsertUserSessionUseCase upsertUserSessionUseCase = new UpsertUserSessionUseCase(userSessionRoutingPort);
        upsertUserSessionUseCase.execute(new UpsertUserSessionRequest(userId, sessionId, serverId));

        ExecutorSessionRegistry sessionRegistry = new ExecutorSessionRegistry();
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        ExecutorEventEventHandler executorEventEventHandler = mock(ExecutorEventEventHandler.class);
        ExecutorWebSocketHandler webSocketHandler = new ExecutorWebSocketHandler(
                objectMapper,
                sessionRegistry,
                subscriptionPersistencePort,
                executorEventEventHandler
        );

        WebSocketSession webSocketSession = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ExecutorHandshakeInterceptor.WS_TOKEN_ATTRIBUTE, wsToken);
        when(webSocketSession.getId()).thenReturn(sessionId);
        when(webSocketSession.getAttributes()).thenReturn(attributes);
        when(webSocketSession.isOpen()).thenReturn(true);

        webSocketHandler.handleTextMessage(
                webSocketSession,
                new TextMessage("""
                        {
                          "type": "subscribe",
                          "payload": {
                            "bot_id": "%s"
                          }
                        }
                        """.formatted(botId))
        );

        assertThat(subscriptionPersistencePort.findActiveByBotIdAndWsToken(botId, wsToken))
                .get()
                .extracting(UserSubscription::isExecutorConnected)
                .isEqualTo(true);

        SignalDispatchKafkaConsumer consumer = new SignalDispatchKafkaConsumer(objectMapper, sessionRegistry);
        RecordingSignalServerDispatchPort signalServerDispatchPort = new RecordingSignalServerDispatchPort(consumer);
        ResolveBotRoutingTargetsUseCase resolveBotRoutingTargetsUseCase = new ResolveBotRoutingTargetsUseCase(
                botSubscriberRoutingPort,
                userSessionRoutingPort
        );
        InMemoryBotRepository inMemoryBotRepository = new InMemoryBotRepository();
        inMemoryBotRepository.registerBot(botId);
        CaptureSignalUseCase captureSignalUseCase = new CaptureSignalUseCase(
                new InMemorySignalRepository(),
                inMemoryBotRepository,
                resolveBotRoutingTargetsUseCase,
                new InMemorySignalPublisherPort(),
                signalServerDispatchPort
        );

        Signal signal = Signal.builder()
                .signalId("sig_1")
                .botId(botId)
                .symbol("BTC/USDT")
                .action(SignalAction.OPEN_LONG)
                .entry(new BigDecimal("123.45"))
                .stopLoss(new BigDecimal("120.00"))
                .takeProfit(new BigDecimal("130.00"))
                .status(SignalStatus.RECEIVED)
                .generatedTimestamp(LocalDateTime.parse("2026-05-01T00:00:00"))
                .metadata(Map.of("strategy", "momentum"))
                .build();

        captureSignalUseCase.execute(signal);

        assertThat(signalServerDispatchPort.dispatchedTargetSets).hasSize(1);
        assertThat(signalServerDispatchPort.dispatchedTargetSets.get(0)).containsExactly(serverId);

        org.mockito.ArgumentCaptor<TextMessage> messageCaptor = org.mockito.ArgumentCaptor.forClass(TextMessage.class);
        verify(webSocketSession, times(2)).sendMessage(messageCaptor.capture());

        List<String> frames = messageCaptor.getAllValues().stream()
                .map(TextMessage::getPayload)
                .toList();

        assertThat(frames.get(0)).contains("\"type\":\"ack\"");
        assertThat(frames.get(0)).contains("\"ack_type\":\"subscribe\"");
        assertThat(frames.get(0)).contains("\"bot_id\":\"" + botId + "\"");

        assertThat(frames.get(1)).contains("\"type\":\"signal\"");
        assertThat(frames.get(1)).contains("\"signal_id\":\"sig_1\"");
        assertThat(frames.get(1)).contains("\"bot_id\":\"" + botId + "\"");
        assertThat(frames.get(1)).contains("\"symbol\":\"BTC/USDT\"");
        assertThat(frames.get(1)).contains("\"action\":\"OPEN_LONG\"");
    }

    private static final class RecordingSignalServerDispatchPort implements SignalServerDispatchPort {

        private final SignalDispatchKafkaConsumer consumer;
        private final List<Set<String>> dispatchedTargetSets = new ArrayList<>();
        private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

        private RecordingSignalServerDispatchPort(SignalDispatchKafkaConsumer consumer) {
            this.consumer = consumer;
        }

        @Override
        public void dispatchToServers(Signal signal, Set<String> serverIds) {
            dispatchedTargetSets.add(new LinkedHashSet<>(serverIds));
            try {
                consumer.consume(objectMapper.writeValueAsString(signal));
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to serialize signal for dispatch test", exception);
            }
        }
    }

    private static final class InMemorySignalRepository implements io.marcus.domain.repository.SignalRepository {

        @Override
        public void save(Signal signal) {
            // no-op for this integration test
        }

        @Override
        public boolean existsBySignalId(String signalId) {
            // no-op for this integration test - always return false to allow signal publishing
            return false;
        }
    }

    private static final class InMemorySignalPublisherPort implements SignalPublisherPort {

        @Override
        public void publish(Signal signal) {
            // no-op for this integration test
        }
    }

    private static final class InMemoryBotSubscriberRoutingPort implements BotSubscriberRoutingPort {

        private final Map<String, Set<String>> subscribersByBot = new LinkedHashMap<>();

        @Override
        public void upsertSubscriber(String botId, String userId) {
            subscribersByBot.computeIfAbsent(botId, ignored -> new LinkedHashSet<>()).add(userId);
        }

        @Override
        public void removeSubscriber(String botId, String userId) {
            Set<String> subscribers = subscribersByBot.get(botId);
            if (subscribers != null) {
                subscribers.remove(userId);
            }
        }

        @Override
        public Set<String> findActiveSubscriberUserIdsByBotId(String botId) {
            return new LinkedHashSet<>(subscribersByBot.getOrDefault(botId, Set.of()));
        }
    }

    private static final class InMemoryUserSessionRoutingPort implements UserSessionRoutingPort {

        private final Map<String, Set<String>> serverIdsByUser = new LinkedHashMap<>();
        private final Map<String, String> sessionToServer = new LinkedHashMap<>();

        @Override
        public void upsertSession(String userId, String sessionId, String serverId) {
            serverIdsByUser.computeIfAbsent(userId, ignored -> new LinkedHashSet<>()).add(serverId);
            sessionToServer.put(sessionId, serverId);
        }

        @Override
        public void removeSession(String userId, String sessionId) {
            String serverId = sessionToServer.remove(sessionId);
            if (serverId == null) {
                return;
            }
            Set<String> serverIds = serverIdsByUser.get(userId);
            if (serverIds != null) {
                serverIds.remove(serverId);
            }
        }

        @Override
        public Set<String> findServerIdsByUserIds(Set<String> userIds) {
            Set<String> serverIds = new LinkedHashSet<>();
            for (String userId : userIds) {
                serverIds.addAll(serverIdsByUser.getOrDefault(userId, Set.of()));
            }
            return serverIds;
        }
    }

    private static final class InMemoryBotRepository implements BotRepository {

        private final Map<String, Bot> botsByBotId = new LinkedHashMap<>();

        private InMemoryBotRepository() {
        }

        public void registerBot(String botId) {
            Bot bot = Bot.builder()
                    .botId(botId)
                    .developerId("dev-user")
                    .status(BotStatus.ACTIVE)
                    .build();
            botsByBotId.put(botId, bot);
        }

        @Override
        public Bot save(Bot bot) {
            botsByBotId.put(bot.getBotId(), bot);
            return bot;
        }

        @Override
        public Optional<Bot> findByBotId(String botId) {
            return Optional.ofNullable(botsByBotId.get(botId));
        }

        @Override
        public List<Bot> findAllActive() {
            return botsByBotId.values().stream()
                    .filter(bot -> bot.getStatus() == BotStatus.ACTIVE)
                    .toList();
        }

        @Override
        public List<Bot> findAllByDeveloperId(String developerId) {
            return botsByBotId.values().stream()
                    .filter(bot -> developerId.equals(bot.getDeveloperId()))
                    .toList();
        }

        @Override
        public Optional<String> findSecretByBotId(String botId) {
            return Optional.ofNullable(botsByBotId.get(botId)).map(Bot::getSecretKey);
        }

        @Override
        public Optional<String> findSecretByApiKey(String apiKey) {
            return botsByBotId.values().stream()
                    .filter(bot -> apiKey.equals(bot.getApiKey()))
                    .map(Bot::getSecretKey)
                    .findFirst();
        }
    }

    private static final class InMemoryUserRepository implements UserRepository {

        private final String userId;

        private InMemoryUserRepository(String userId) {
            this.userId = userId;
        }

        @Override
        public boolean existsById(String id) {
            return userId.equals(id);
        }

        @Override
        public boolean existsByIdAndRole(String id, Role role) {
            return userId.equals(id) && role == Role.USER;
        }
    }

    private static final class InMemoryUserSubscriptionPersistencePort implements UserSubscriptionPersistencePort {

        private final Map<String, UserSubscription> subscriptionsById = new LinkedHashMap<>();
        private final Map<String, String> wsTokenByUserId = new LinkedHashMap<>();

        private InMemoryUserSubscriptionPersistencePort(Map<String, String> seededWsTokensByUserId) {
            wsTokenByUserId.putAll(seededWsTokensByUserId);
        }

        @Override
        public UserSubscription save(UserSubscription userSubscription) {
            subscriptionsById.put(userSubscription.getUserSubscriptionId(), userSubscription);
            wsTokenByUserId.put(userSubscription.getUserId(), userSubscription.getWsToken());
            return userSubscription;
        }

        @Override
        public Optional<UserSubscription> findActiveByUserIdAndBotId(String userId, String botId) {
            return subscriptionsById.values().stream()
                    .filter(subscription -> subscription.getStatus() == SubscriptionStatus.ACTIVE
                    && userId.equals(subscription.getUserId())
                    && botId.equals(subscription.getBotId()))
                    .findFirst();
        }

        @Override
        public List<UserSubscription> findActiveByUserId(String userId) {
            return subscriptionsById.values().stream()
                    .filter(subscription -> subscription.getStatus() == SubscriptionStatus.ACTIVE
                    && userId.equals(subscription.getUserId()))
                    .toList();
        }

        @Override
        public Optional<String> findAnyActiveWsTokenByUserId(String userId) {
            String preferredWsToken = wsTokenByUserId.get(userId);
            if (preferredWsToken != null && !preferredWsToken.isBlank()) {
                return Optional.of(preferredWsToken);
            }

            return subscriptionsById.values().stream()
                    .filter(subscription -> subscription.getStatus() == SubscriptionStatus.ACTIVE
                    && userId.equals(subscription.getUserId()))
                    .map(UserSubscription::getWsToken)
                    .filter(wsToken -> wsToken != null && !wsToken.isBlank())
                    .findFirst();
        }

        @Override
        public void cancelActiveByUserIdAndBotId(String userId, String botId) {
            subscriptionsById.values().forEach(subscription -> {
                if (subscription.getStatus() == SubscriptionStatus.ACTIVE
                        && userId.equals(subscription.getUserId())
                        && botId.equals(subscription.getBotId())) {
                    subscription.setStatus(SubscriptionStatus.CANCELED);
                }
            });
        }

        @Override
        public List<UserSubscription> findActiveByBotId(String botId) {
            return subscriptionsById.values().stream()
                    .filter(subscription -> subscription.getStatus() == SubscriptionStatus.ACTIVE
                    && botId.equals(subscription.getBotId()))
                    .toList();
        }

        @Override
        public Optional<UserSubscription> findActiveByBotIdAndWsToken(String botId, String wsToken) {
            return subscriptionsById.values().stream()
                    .filter(subscription -> subscription.getStatus() == SubscriptionStatus.ACTIVE
                    && botId.equals(subscription.getBotId())
                    && wsToken.equals(subscription.getWsToken()))
                    .findFirst();
        }

        @Override
        public void markExecutorConnected(String userSubscriptionId, boolean connected) {
            UserSubscription subscription = subscriptionsById.get(userSubscriptionId);
            if (subscription != null) {
                subscription.setExecutorConnected(connected);
            }
        }
    }
}
