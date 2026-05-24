package io.marcus.infrastructure.integration;

import io.marcus.domain.port.BotDiscoveryReadPort.BotDetailSnapshot;
import io.marcus.domain.port.BotDiscoveryReadPort.BotDiscoveryPageSnapshot;
import io.marcus.domain.vo.BotStatus;
import io.marcus.domain.vo.SignalAction;
import io.marcus.domain.vo.SubscriptionStatus;
import io.marcus.infrastructure.persistence.SpringDataBotRepository;
import io.marcus.infrastructure.persistence.SpringDataSignalRepository;
import io.marcus.infrastructure.persistence.SpringDataUserSubscriptionRepository;
import io.marcus.infrastructure.persistence.entity.BotEntity;
import io.marcus.infrastructure.persistence.entity.ExchangeEntity;
import io.marcus.infrastructure.persistence.entity.SignalEntity;
import io.marcus.infrastructure.persistence.entity.UserSubscriptionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaticBotDiscoveryReadAdapterTest {

    @Mock
    private SpringDataBotRepository springDataBotRepository;
    @Mock
    private SpringDataSignalRepository springDataSignalRepository;
    @Mock
    private SpringDataUserSubscriptionRepository springDataUserSubscriptionRepository;

    private StaticBotDiscoveryReadAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new StaticBotDiscoveryReadAdapter(
                springDataBotRepository,
                springDataSignalRepository,
                springDataUserSubscriptionRepository
        );
    }

    @Test
    void getBotDetail_withNullOrBlankId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> adapter.getBotDetail(null));
        assertThrows(IllegalArgumentException.class, () -> adapter.getBotDetail("   "));
    }

    @Test
    void getBotDetail_notFound_throwsException() {
        when(springDataBotRepository.findByBotIdWithExchange("invalid-id"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> adapter.getBotDetail("invalid-id"));
    }

    @Test
    void getBotDetail_happyPath_calculatesMetricsAndActiveSubscriptions() {
        String botId = "bot-123";
        BotEntity bot = BotEntity.builder()
                .botId(botId)
                .name("Test Bot")
                .description("Test Description")
                .status(BotStatus.ACTIVE)
                .tradingPair("BTC/USDT")
                .exchange(ExchangeEntity.builder().exchangeId("binance").name("Binance").build())
                .developerId("dev-1")
                .apiKey("ak_test")
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(LocalDateTime.now())
                .build();

        when(springDataBotRepository.findByBotIdWithExchange(botId))
                .thenReturn(Optional.of(bot));

        // Create mock signals to test SignalMetricsCalculator integration
        SignalEntity s1 = SignalEntity.builder()
                .botId(botId)
                .action(SignalAction.OPEN_LONG)
                .entry(BigDecimal.valueOf(100.0))
                .takeProfit(BigDecimal.valueOf(105.0))
                .stopLoss(BigDecimal.valueOf(95.0))
                .generatedTimestamp(LocalDateTime.now().minusDays(5))
                .build();

        SignalEntity s2 = SignalEntity.builder()
                .botId(botId)
                .action(SignalAction.OPEN_LONG)
                .entry(BigDecimal.valueOf(100.0))
                .takeProfit(BigDecimal.valueOf(110.0))
                .stopLoss(BigDecimal.valueOf(98.0))
                .generatedTimestamp(LocalDateTime.now().minusDays(2))
                .build();

        when(springDataSignalRepository.findByBotIdAndGeneratedTimestampIsNotNullOrderByGeneratedTimestampAsc(botId))
                .thenReturn(List.of(s1, s2));

        // Mock 2 active subscriptions
        UserSubscriptionEntity sub1 = new UserSubscriptionEntity();
        UserSubscriptionEntity sub2 = new UserSubscriptionEntity();
        when(springDataUserSubscriptionRepository.findByBotIdAndStatusOrderByCreatedAtDesc(botId, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(sub1, sub2));

        BotDetailSnapshot detail = adapter.getBotDetail(botId);

        assertNotNull(detail);
        assertEquals(botId, detail.botId());
        assertEquals("Test Bot", detail.botName());
        assertEquals("Test Description", detail.description());
        assertEquals("ACTIVE", detail.status());
        assertEquals("BTC/USDT", detail.tradingPair());
        assertEquals("BINANCE", detail.exchange());
        assertEquals("dev-1", detail.developerId());
        assertEquals("ak_test", detail.apiKey());

        assertNotNull(detail.performance());
        // (105-100)/100 = 5% return for s1, (110-100)/100 = 10% return for s2 -> Avg = 7.5% (0.075)
        assertEquals(0.075, detail.performance().annualReturn(), 0.0001);
        // Max drawdown: s1: (100-95)/100 = 5%, s2: (100-98)/100 = 2% -> Max = 5% (0.05)
        assertEquals(0.05, detail.performance().maxDrawdown(), 0.0001);
        // Profitable trades count = 2 / total = 2 -> 1.0 (100%)
        assertEquals(1.0, detail.performance().winRate(), 0.0001);
        assertEquals(1.25, detail.performance().sharpe(), 0.0001);
        assertEquals(0.075, detail.performance().avgTradeReturn(), 0.0001);
        // Span = minusDays(5) to minusDays(2) = 3 days + 1 = 4 days. Total trades = 2 -> 2/4 = 0.5
        assertEquals(0.5, detail.performance().tradesPerDay(), 0.0001);
    }

    @Test
    void listPublicBots_returnsPagedFilteredResults() {
        String botId = "bot-123";
        BotEntity bot = BotEntity.builder()
                .botId(botId)
                .name("Kinetic Alpha")
                .description("Test Description")
                .status(BotStatus.ACTIVE)
                .tradingPair("BTC/USDT")
                .exchange(ExchangeEntity.builder().exchangeId("binance").name("Binance").build())
                .build();

        when(springDataBotRepository.findAllWithExchange())
                .thenReturn(List.of(bot));

        when(springDataSignalRepository.findByBotIdAndGeneratedTimestampIsNotNullOrderByGeneratedTimestampAsc(botId))
                .thenReturn(List.of());

        when(springDataUserSubscriptionRepository.findByBotIdAndStatusOrderByCreatedAtDesc(botId, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of());

        // Test filter matching name "kinetic"
        BotDiscoveryPageSnapshot page = adapter.listPublicBots("kinetic", "ALL", "ALL", "DEFAULT", 0, 10);

        assertNotNull(page);
        assertEquals(1, page.items().size());
        assertEquals("Kinetic Alpha", page.items().get(0).botName());
        assertEquals(0, page.meta().page());
        assertEquals(10, page.meta().size());
        assertEquals(1, page.meta().totalElements());

        // Test non-matching query
        BotDiscoveryPageSnapshot pageEmpty = adapter.listPublicBots("different", "ALL", "ALL", "DEFAULT", 0, 10);
        assertTrue(pageEmpty.items().isEmpty());
    }

    @Test
    void listPublicBots_excludesSimulatedSignalsFromMetrics() {
        String botId = "bot-123";
        BotEntity bot = BotEntity.builder()
                .botId(botId)
                .name("Kinetic Alpha")
                .description("Test Description")
                .status(BotStatus.ACTIVE)
                .tradingPair("BTC/USDT")
                .exchange(ExchangeEntity.builder().exchangeId("binance").name("Binance").build())
                .build();

        when(springDataBotRepository.findAllWithExchange())
                .thenReturn(List.of(bot));

        // Create one simulated and one real signal
        java.util.Map<String, Object> simMetadata = new java.util.HashMap<>();
        simMetadata.put("simulation", true);

        SignalEntity sReal = SignalEntity.builder()
                .botId(botId)
                .action(SignalAction.OPEN_LONG)
                .entry(BigDecimal.valueOf(100.0))
                .takeProfit(BigDecimal.valueOf(110.0))
                .stopLoss(BigDecimal.valueOf(90.0))
                .generatedTimestamp(LocalDateTime.now().minusDays(5))
                .metadata(new java.util.HashMap<>())
                .build();

        SignalEntity sSim = SignalEntity.builder()
                .botId(botId)
                .action(SignalAction.OPEN_LONG)
                .entry(BigDecimal.valueOf(100.0))
                .takeProfit(BigDecimal.valueOf(200.0)) // very high performance
                .stopLoss(BigDecimal.valueOf(90.0))
                .generatedTimestamp(LocalDateTime.now().minusDays(1))
                .metadata(simMetadata)
                .build();

        when(springDataSignalRepository.findByBotIdAndGeneratedTimestampIsNotNullOrderByGeneratedTimestampAsc(botId))
                .thenReturn(List.of(sReal, sSim));

        when(springDataUserSubscriptionRepository.findByBotIdAndStatusOrderByCreatedAtDesc(botId, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of());

        // Test listPublicBots
        BotDiscoveryPageSnapshot page = adapter.listPublicBots("kinetic", "ALL", "ALL", "DEFAULT", 0, 10);

        assertNotNull(page);
        assertEquals(1, page.items().size());
        // Annual return should only consider sReal (10% or 0.1), not the simulated one
        assertEquals(0.1, page.items().get(0).annualReturn(), 0.0001);
    }
}
