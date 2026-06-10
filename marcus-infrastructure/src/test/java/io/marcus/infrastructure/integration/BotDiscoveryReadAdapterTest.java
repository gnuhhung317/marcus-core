package io.marcus.infrastructure.integration;

import io.marcus.domain.port.BotDiscoveryReadPort.TradeLogPageSnapshot;
import io.marcus.domain.port.BotDiscoveryReadPort.TradeLogSnapshot;
import io.marcus.domain.vo.SubscriptionStatus;
import io.marcus.domain.vo.SignalAction;
import io.marcus.domain.service.IdentityService;
import io.marcus.infrastructure.persistence.*;
import io.marcus.infrastructure.persistence.entity.BotEntity;
import io.marcus.infrastructure.persistence.entity.SignalEntity;
import io.marcus.infrastructure.persistence.entity.UserSubscriptionEntity;
import io.marcus.infrastructure.persistence.executor.ExecutionEventEntity;
import io.marcus.infrastructure.persistence.executor.ExecutionEventRepository;
import io.marcus.infrastructure.persistence.executor.ExecutionStateEntity;
import io.marcus.infrastructure.persistence.executor.ExecutionStateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BotDiscoveryReadAdapterTest {

    @Mock
    private SpringDataBotRepository springDataBotRepository;
    @Mock
    private SpringDataUserRepository springDataUserRepository;
    @Mock
    private SpringDataUserSubscriptionRepository springDataUserSubscriptionRepository;
    @Mock
    private SpringDataBotDryRunClosedTradeRepository botDryRunClosedTradeRepository;
    @Mock
    private SpringDataBotHistoricalClosedTradeRepository botHistoricalClosedTradeRepository;
    @Mock
    private SpringDataLeaderboardMetricsRepository leaderboardMetricsRepository;
    @Mock
    private SpringDataBotFavoriteRepository botFavoriteRepository;
    @Mock
    private SpringDataSignalRepository springDataSignalRepository;
    @Mock
    private IdentityService identityService;
    @Mock
    private ExecutionStateRepository executionStateRepository;
    @Mock
    private ExecutionEventRepository executionEventRepository;

    private BotDiscoveryReadAdapter adapter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        adapter = new BotDiscoveryReadAdapter(
                springDataBotRepository,
                springDataSignalRepository,
                springDataUserSubscriptionRepository,
                springDataUserRepository,
                leaderboardMetricsRepository,
                botDryRunClosedTradeRepository,
                botHistoricalClosedTradeRepository,
                botFavoriteRepository,
                identityService,
                executionStateRepository,
                executionEventRepository
        );
    }

    @Test
    void listBotTrades_unauthenticated_returnsEmpty() {
        String botId = "bot-1";
        BotEntity bot = new BotEntity();
        bot.setBotId(botId);
        when(springDataBotRepository.findByBotId(botId)).thenReturn(Optional.of(bot));
        when(identityService.getCurrentUserId()).thenReturn(Optional.empty());

        TradeLogPageSnapshot snapshot = adapter.listBotTrades(botId, 0, 10, null);

        assertNotNull(snapshot);
        assertTrue(snapshot.items().isEmpty());
        assertEquals(0, snapshot.totalElements());
    }

    @Test
    void listBotTrades_notSubscribed_returnsEmpty() {
        String botId = "bot-1";
        String userId = "user-1";
        BotEntity bot = new BotEntity();
        bot.setBotId(botId);
        when(springDataBotRepository.findByBotId(botId)).thenReturn(Optional.of(bot));
        when(identityService.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(springDataUserSubscriptionRepository.findByUserIdAndBotIdAndStatus(userId, botId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        TradeLogPageSnapshot snapshot = adapter.listBotTrades(botId, 0, 10, null);

        assertNotNull(snapshot);
        assertTrue(snapshot.items().isEmpty());
        assertEquals(0, snapshot.totalElements());
    }

    @Test
    void listBotTrades_subscribed_returnsExecutorTrades() {
        String botId = "bot-1";
        String userId = "user-1";
        BotEntity bot = new BotEntity();
        bot.setBotId(botId);

        when(springDataBotRepository.findByBotId(botId)).thenReturn(Optional.of(bot));
        when(identityService.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(springDataUserSubscriptionRepository.findByUserIdAndBotIdAndStatus(userId, botId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(new UserSubscriptionEntity()));

        ExecutionStateEntity es = new ExecutionStateEntity(
                "sig-1",
                "CLOSED",
                "FILLED",
                "CLOSED",
                1,
                Instant.parse("2026-06-10T12:00:00Z"),
                Instant.parse("2026-06-10T12:00:00Z")
        );

        SignalEntity s = new SignalEntity();
        s.setSignalId("sig-1");
        s.setSymbol("BTC/USDT");
        s.setAction(SignalAction.OPEN_LONG);
        s.setAmount(BigDecimal.valueOf(1.5));
        s.setEntry(BigDecimal.valueOf(60000.0));

        List<Object[]> queryResults = new ArrayList<>();
        queryResults.add(new Object[]{es, s});

        when(executionStateRepository.findClosedExecutionStatesAndSignalsForBot(botId, null))
                .thenReturn(queryResults);

        ObjectNode filledPayload = objectMapper.createObjectNode();
        filledPayload.put("fill_price", 60000.0);
        ExecutionEventEntity filledEvent = new ExecutionEventEntity(
                "evt-1",
                "sig-1",
                0,
                "ORDER_FILLED",
                Instant.now(),
                Instant.now(),
                filledPayload,
                Instant.now()
        );

        ObjectNode closedPayload = objectMapper.createObjectNode();
        closedPayload.put("pnl", 1500.0);
        closedPayload.put("exit_price", 61000.0);
        ExecutionEventEntity closedEvent = new ExecutionEventEntity(
                "evt-2",
                "sig-1",
                1,
                "POSITION_CLOSED",
                Instant.now(),
                Instant.now(),
                closedPayload,
                Instant.now()
        );

        when(executionEventRepository.findBySignalIdOrderBySequenceAsc("sig-1"))
                .thenReturn(List.of(filledEvent, closedEvent));

        TradeLogPageSnapshot snapshot = adapter.listBotTrades(botId, 0, 10, null);

        assertNotNull(snapshot);
        assertEquals(1, snapshot.totalElements());
        TradeLogSnapshot item = snapshot.items().get(0);
        assertEquals("BTC/USDT", item.assetPair());
        assertEquals("LONG", item.side());
        assertEquals(1.5, item.size());
        assertEquals(60000.0, item.entryPrice());
        assertEquals(61000.0, item.exitPrice());
        assertEquals(1500.0, item.netPnl());
    }
}
