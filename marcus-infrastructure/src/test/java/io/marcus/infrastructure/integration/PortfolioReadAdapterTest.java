package io.marcus.infrastructure.integration;

import io.marcus.domain.port.PortfolioReadPort.ExecutionLogPageSnapshot;
import io.marcus.domain.port.PortfolioReadPort.ExecutionLogItemSnapshot;
import io.marcus.domain.port.PortfolioReadPort.BotIntegrationHealthSnapshot;
import io.marcus.domain.port.PortfolioReadPort.ConnectivityHealthSnapshot;
import io.marcus.domain.port.PortfolioReadPort;
import io.marcus.domain.port.ExecutorOnlineStatusPort;
import io.marcus.domain.port.PortfolioReadPort.SubscriptionDecisionSnapshot;
import io.marcus.domain.vo.BotStatus;
import io.marcus.domain.vo.MarketType;
import io.marcus.domain.vo.OrderType;
import io.marcus.domain.vo.SignalAction;
import io.marcus.domain.vo.SignalStatus;
import io.marcus.domain.vo.SubscriptionStatus;
import io.marcus.infrastructure.persistence.entity.BotEntity;
import io.marcus.infrastructure.persistence.entity.ExchangeEntity;
import io.marcus.infrastructure.persistence.entity.PortfolioAccountEntity;
import io.marcus.infrastructure.persistence.entity.PortfolioAggregateHistoryEntity;
import io.marcus.infrastructure.persistence.entity.PortfolioBalanceHistoryEntity;
import io.marcus.infrastructure.persistence.entity.SignalEntity;
import io.marcus.infrastructure.persistence.SpringDataBotRepository;
import io.marcus.infrastructure.persistence.SpringDataPortfolioAccountRepository;
import io.marcus.infrastructure.persistence.SpringDataPortfolioAggregateHistoryRepository;
import io.marcus.infrastructure.persistence.SpringDataPortfolioHistoryRepository;
import io.marcus.infrastructure.persistence.SpringDataRawEventRepository;
import io.marcus.infrastructure.persistence.SpringDataSignalRepository;
import io.marcus.infrastructure.persistence.SpringDataUserSubscriptionRepository;
import io.marcus.infrastructure.persistence.SpringDataUserPortfolioRepository;
import io.marcus.infrastructure.persistence.entity.RawEventEntity;
import io.marcus.infrastructure.persistence.entity.UserPortfolioEntity;
import io.marcus.infrastructure.persistence.entity.UserSubscriptionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioReadAdapterTest {

    @Mock
    private SpringDataBotRepository springDataBotRepository;
    @Mock
    private SpringDataSignalRepository springDataSignalRepository;
    @Mock
    private SpringDataUserSubscriptionRepository springDataUserSubscriptionRepository;
    @Mock
    private SpringDataUserPortfolioRepository springDataUserPortfolioRepository;
    @Mock
    private SpringDataRawEventRepository springDataRawEventRepository;
    @Mock
    private SpringDataPortfolioAggregateHistoryRepository springDataPortfolioAggregateHistoryRepository;
    @Mock
    private SpringDataPortfolioAccountRepository springDataPortfolioAccountRepository;
    @Mock
    private SpringDataPortfolioHistoryRepository springDataPortfolioHistoryRepository;
    @Mock
    private ExecutorOnlineStatusPort executorOnlineStatusPort;

    private PortfolioReadAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PortfolioReadAdapter(
                springDataBotRepository,
                springDataSignalRepository,
                springDataUserSubscriptionRepository,
                springDataUserPortfolioRepository,
                springDataPortfolioAccountRepository,
                springDataPortfolioHistoryRepository,
                springDataRawEventRepository,
                springDataPortfolioAggregateHistoryRepository,
                executorOnlineStatusPort
        );
    }

    @Test
    void listSystemExecutionLogs_withNullCursor_defaultsToZeroOffset() {
        int limit = 5;
        when(springDataRawEventRepository.findSystemExecutionLogs(6, 0))
                .thenReturn(List.of());

        ExecutionLogPageSnapshot snapshot = adapter.listSystemExecutionLogs(null, limit);

        assertNotNull(snapshot);
        assertTrue(snapshot.items().isEmpty());
        assertNull(snapshot.cursor());
        verify(springDataRawEventRepository).findSystemExecutionLogs(6, 0);
    }

    @Test
    void listSystemExecutionLogs_withValidCursor_parsesOffset() {
        int limit = 5;
        when(springDataRawEventRepository.findSystemExecutionLogs(6, 10))
                .thenReturn(List.of());

        ExecutionLogPageSnapshot snapshot = adapter.listSystemExecutionLogs("10", limit);

        assertNotNull(snapshot);
        assertTrue(snapshot.items().isEmpty());
        assertNull(snapshot.cursor());
        verify(springDataRawEventRepository).findSystemExecutionLogs(6, 10);
    }

    @Test
    void listSystemExecutionLogs_withInvalidCursor_defaultsToZeroOffset() {
        int limit = 5;
        when(springDataRawEventRepository.findSystemExecutionLogs(6, 0))
                .thenReturn(List.of());

        ExecutionLogPageSnapshot snapshot = adapter.listSystemExecutionLogs("not_a_number", limit);

        assertNotNull(snapshot);
        assertTrue(snapshot.items().isEmpty());
        assertNull(snapshot.cursor());
        verify(springDataRawEventRepository).findSystemExecutionLogs(6, 0);
    }

    @Test
    void listSystemExecutionLogs_hasMore_returnsNextCursor() {
        int limit = 2;
        RawEventEntity e1 = RawEventEntity.builder()
                .id("1")
                .eventId("evt-1")
                .botId("bot-1")
                .type("ingest")
                .payload(Map.of("action", "BUY", "symbol", "BTC/USDT", "price", 60000.0))
                .receivedAt(Instant.parse("2026-05-22T10:00:00Z"))
                .sourceConnId("conn-1")
                .sequenceNo(1L)
                .processed(true)
                .build();
        RawEventEntity e2 = RawEventEntity.builder()
                .id("2")
                .eventId("evt-2")
                .botId("bot-1")
                .type("heartbeat")
                .payload(Map.of())
                .receivedAt(Instant.parse("2026-05-22T09:59:00Z"))
                .sourceConnId("conn-1")
                .sequenceNo(2L)
                .processed(true)
                .build();
        RawEventEntity e3 = RawEventEntity.builder()
                .id("3")
                .eventId("evt-3")
                .botId("bot-2")
                .type("error")
                .payload(Map.of("error", "Failed to decode"))
                .receivedAt(Instant.parse("2026-05-22T09:58:00Z"))
                .sourceConnId("conn-2")
                .sequenceNo(3L)
                .processed(true)
                .build();

        when(springDataRawEventRepository.findSystemExecutionLogs(3, 0))
                .thenReturn(List.of(e1, e2, e3));

        ExecutionLogPageSnapshot snapshot = adapter.listSystemExecutionLogs("0", limit);

        assertNotNull(snapshot);
        assertEquals(2, snapshot.items().size());
        assertEquals("2", snapshot.cursor()); // limit = 2, offset = 0, next offset = 2
        
        ExecutionLogItemSnapshot item1 = snapshot.items().get(0);
        assertEquals("INFO", item1.level());
        assertEquals("bot-1", item1.source());
        assertTrue(item1.message().contains("Signal Ingested: BUY BTC/USDT @ 60000.0"));
        
        ExecutionLogItemSnapshot item2 = snapshot.items().get(1);
        assertEquals("INFO", item2.level());
        assertEquals("bot-1", item2.source());
        assertTrue(item2.message().contains("Heartbeat received"));
    }

    @Test
    void listSystemExecutionLogs_mappingDifferentLevelsAndTypes() {
        int limit = 10;
        RawEventEntity e1 = RawEventEntity.builder()
                .id("1")
                .eventId("evt-1")
                .botId("")
                .type("audit-push")
                .payload(Map.of("kind", "balance_check"))
                .receivedAt(Instant.parse("2026-05-22T10:00:00Z"))
                .sourceConnId("conn-1")
                .sequenceNo(1L)
                .processed(true)
                .build();
        RawEventEntity e2 = RawEventEntity.builder()
                .id("2")
                .eventId("evt-2")
                .botId("bot-1")
                .type("ack")
                .payload(Map.of("ackEventId", "evt-abc", "status", "FAILED"))
                .receivedAt(Instant.parse("2026-05-22T09:59:00Z"))
                .sourceConnId("conn-1")
                .sequenceNo(2L)
                .processed(true)
                .build();

        when(springDataRawEventRepository.findSystemExecutionLogs(11, 0))
                .thenReturn(List.of(e1, e2));

        ExecutionLogPageSnapshot snapshot = adapter.listSystemExecutionLogs(null, limit);

        assertNotNull(snapshot);
        assertEquals(2, snapshot.items().size());
        
        ExecutionLogItemSnapshot item1 = snapshot.items().get(0);
        assertEquals("INFO", item1.level());
        assertEquals("system", item1.source()); // botId is empty, falls back to "system"
        assertTrue(item1.message().contains("Audit Push: balance_check"));

        ExecutionLogItemSnapshot item2 = snapshot.items().get(1);
        assertEquals("ERROR", item2.level()); // status FAILED mapping to ERROR
        assertEquals("bot-1", item2.source());
        assertTrue(item2.message().contains("Acknowledgment received for EventID: evt-abc"));
    }

    @Test
    void getBotIntegrationHealth_botNotFound_throwsNoSuchElementException() {
        when(springDataBotRepository.findByBotId("bot-invalid")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> adapter.getBotIntegrationHealth("bot-invalid"));
    }

    @Test
    void getBotIntegrationHealth_healthyBot_returnsUp() {
        String botId = "bot-123";
        BotEntity bot = BotEntity.builder().botId(botId).build();
        when(springDataBotRepository.findByBotId(botId)).thenReturn(Optional.of(bot));

        // Heartbeat is extremely fresh (5 seconds ago)
        RawEventEntity heartbeat = RawEventEntity.builder()
                .botId(botId)
                .type("heartbeat")
                .receivedAt(Instant.now().minusSeconds(5))
                .build();
        when(springDataRawEventRepository.findLatestHeartbeatForBot(botId)).thenReturn(Optional.of(heartbeat));

        // Signal generated 10 minutes ago
        SignalEntity signal = new SignalEntity();
        signal.setBotId(botId);
        signal.setGeneratedTimestamp(LocalDateTime.now().minusMinutes(10));
        when(springDataSignalRepository.findByBotId(botId)).thenReturn(List.of(signal));

        BotIntegrationHealthSnapshot snapshot = adapter.getBotIntegrationHealth(botId);

        assertNotNull(snapshot);
        assertEquals("UP", snapshot.overallStatus());
        assertTrue(snapshot.message().contains("healthy"));
    }

    @Test
    void getBotIntegrationHealth_degradedHeartbeat_returnsDegraded() {
        String botId = "bot-123";
        BotEntity bot = BotEntity.builder().botId(botId).build();
        when(springDataBotRepository.findByBotId(botId)).thenReturn(Optional.of(bot));

        // Heartbeat is somewhat old (8.3 minutes / 500 seconds ago)
        RawEventEntity heartbeat = RawEventEntity.builder()
                .botId(botId)
                .type("heartbeat")
                .receivedAt(Instant.now().minusSeconds(500))
                .build();
        when(springDataRawEventRepository.findLatestHeartbeatForBot(botId)).thenReturn(Optional.of(heartbeat));
        when(springDataSignalRepository.findByBotId(botId)).thenReturn(List.of());

        BotIntegrationHealthSnapshot snapshot = adapter.getBotIntegrationHealth(botId);

        assertNotNull(snapshot);
        assertEquals("DEGRADED", snapshot.overallStatus());
        assertTrue(snapshot.message().contains("degraded") || snapshot.message().contains("latency"));
    }

    @Test
    void getBotIntegrationHealth_offlineHeartbeat_returnsDown() {
        String botId = "bot-123";
        BotEntity bot = BotEntity.builder().botId(botId).build();
        when(springDataBotRepository.findByBotId(botId)).thenReturn(Optional.of(bot));

        // Heartbeat is very old (16.6 minutes / 1000 seconds ago)
        RawEventEntity heartbeat = RawEventEntity.builder()
                .botId(botId)
                .type("heartbeat")
                .receivedAt(Instant.now().minusSeconds(1000))
                .build();
        when(springDataRawEventRepository.findLatestHeartbeatForBot(botId)).thenReturn(Optional.of(heartbeat));
        when(springDataSignalRepository.findByBotId(botId)).thenReturn(List.of());

        BotIntegrationHealthSnapshot snapshot = adapter.getBotIntegrationHealth(botId);

        assertNotNull(snapshot);
        assertEquals("DOWN", snapshot.overallStatus());
        assertTrue(snapshot.message().contains("offline"));
    }

    @Test
    void getBotIntegrationHealth_staleSignal_returnsUp() {
        String botId = "bot-123";
        BotEntity bot = BotEntity.builder().botId(botId).build();
        when(springDataBotRepository.findByBotId(botId)).thenReturn(Optional.of(bot));

        // Heartbeat is fresh
        RawEventEntity heartbeat = RawEventEntity.builder()
                .botId(botId)
                .type("heartbeat")
                .receivedAt(Instant.now().minusSeconds(10))
                .build();
        when(springDataRawEventRepository.findLatestHeartbeatForBot(botId)).thenReturn(Optional.of(heartbeat));

        // Signal was generated 2 days (48 hours) ago
        SignalEntity signal = new SignalEntity();
        signal.setBotId(botId);
        signal.setGeneratedTimestamp(LocalDateTime.now().minusDays(2));
        when(springDataSignalRepository.findByBotId(botId)).thenReturn(List.of(signal));

        BotIntegrationHealthSnapshot snapshot = adapter.getBotIntegrationHealth(botId);

        assertNotNull(snapshot);
        assertEquals("UP", snapshot.overallStatus());
        assertTrue(snapshot.message().contains("System is fully healthy"));
    }

    @Test
    void getSystemConnectivityHealth_freshHeartbeat_returnsUp() {
        RawEventEntity heartbeat = RawEventEntity.builder()
                .type("heartbeat")
                .receivedAt(Instant.now().minusSeconds(30))
                .build();
        when(springDataRawEventRepository.findLatestHeartbeat()).thenReturn(Optional.of(heartbeat));
        when(executorOnlineStatusPort.isAnyOnline()).thenReturn(true);

        ConnectivityHealthSnapshot snapshot = adapter.getSystemConnectivityHealth();

        assertNotNull(snapshot);
        assertEquals("UP", snapshot.overallStatus());
        assertEquals("UP", snapshot.executorConnectionStatus());
        assertEquals("UP", snapshot.heartbeatStatus());
        assertNotNull(snapshot.lastHeartbeatAt());
    }

    @Test
    void getSystemConnectivityHealth_staleHeartbeat_returnsDegraded() {
        RawEventEntity heartbeat = RawEventEntity.builder()
                .type("heartbeat")
                .receivedAt(Instant.now().minusSeconds(500))
                .build();
        when(springDataRawEventRepository.findLatestHeartbeat()).thenReturn(Optional.of(heartbeat));
        when(executorOnlineStatusPort.isAnyOnline()).thenReturn(true);

        ConnectivityHealthSnapshot snapshot = adapter.getSystemConnectivityHealth();

        assertNotNull(snapshot);
        assertEquals("DEGRADED", snapshot.overallStatus());
        assertEquals("UP", snapshot.executorConnectionStatus());
        assertEquals("DEGRADED", snapshot.heartbeatStatus());
    }

    @Test
    void getSystemConnectivityHealth_missingHeartbeat_returnsDown() {
        when(springDataRawEventRepository.findLatestHeartbeat()).thenReturn(Optional.empty());

        ConnectivityHealthSnapshot snapshot = adapter.getSystemConnectivityHealth();

        assertNotNull(snapshot);
        assertEquals("DOWN", snapshot.overallStatus());
        assertEquals("DOWN", snapshot.executorConnectionStatus());
        assertEquals("DOWN", snapshot.heartbeatStatus());
    }

    @Test
    void getSystemConnectivityHealth_onlineExecutorWithoutHeartbeat_returnsDegraded() {
        when(springDataRawEventRepository.findLatestHeartbeat()).thenReturn(Optional.empty());
        when(executorOnlineStatusPort.isAnyOnline()).thenReturn(true);

        ConnectivityHealthSnapshot snapshot = adapter.getSystemConnectivityHealth();

        assertEquals("DEGRADED", snapshot.overallStatus());
        assertEquals("UP", snapshot.executorConnectionStatus());
        assertEquals("DOWN", snapshot.heartbeatStatus());
        assertNull(snapshot.lastHeartbeatAt());
    }

    @Test
    void getSystemConnectivityHealth_offlineExecutorWithFreshHeartbeat_returnsDown() {
        RawEventEntity heartbeat = RawEventEntity.builder()
                .type("heartbeat")
                .receivedAt(Instant.now().minusSeconds(30))
                .build();
        when(springDataRawEventRepository.findLatestHeartbeat()).thenReturn(Optional.of(heartbeat));
        when(executorOnlineStatusPort.isAnyOnline()).thenReturn(false);

        ConnectivityHealthSnapshot snapshot = adapter.getSystemConnectivityHealth();

        assertEquals("DOWN", snapshot.overallStatus());
        assertEquals("DOWN", snapshot.executorConnectionStatus());
        assertEquals("UP", snapshot.heartbeatStatus());
    }

    @Test
    void listDashboardEquitySeries_usesAggregateHistory() {
        LocalDateTime nowBefore = LocalDateTime.now();
        when(springDataPortfolioAggregateHistoryRepository.findByUserIdAndSnapshotAtAfterOrderBySnapshotAtAsc(eq("usr-1"), any()))
                .thenReturn(List.of(
                        PortfolioAggregateHistoryEntity.builder()
                                .snapshotAt(LocalDateTime.of(2026, 5, 1, 10, 0))
                                .total(new BigDecimal("10000"))
                                .build(),
                        PortfolioAggregateHistoryEntity.builder()
                                .snapshotAt(LocalDateTime.of(2026, 5, 1, 10, 5))
                                .total(new BigDecimal("15000"))
                                .build()
                ));
        when(springDataUserPortfolioRepository.findByUserId("usr-1"))
                .thenReturn(Optional.of(UserPortfolioEntity.builder()
                        .userId("usr-1")
                        .totalCapital(new BigDecimal("15000"))
                        .build()));

        var series = adapter.listDashboardEquitySeries("usr-1", "1D");
        LocalDateTime nowAfter = LocalDateTime.now();

        assertEquals(3, series.size());
        assertEquals(LocalDateTime.of(2026, 5, 1, 10, 0), series.get(0).timestamp());
        assertEquals(10000.0, series.get(0).value());
        assertEquals(LocalDateTime.of(2026, 5, 1, 10, 5), series.get(1).timestamp());
        assertEquals(15000.0, series.get(1).value());
        assertRange(series.get(2).timestamp(), nowBefore, nowAfter);
        assertEquals(15000.0, series.get(2).value());
    }

    @Test
    void listDashboardEquitySeries_withoutHistory_returnsFlatSeriesToNow() {
        LocalDateTime nowBefore = LocalDateTime.now();
        when(springDataPortfolioAggregateHistoryRepository.findByUserIdAndSnapshotAtAfterOrderBySnapshotAtAsc(eq("usr-1"), any()))
                .thenReturn(List.of());
        when(springDataUserPortfolioRepository.findByUserId("usr-1"))
                .thenReturn(Optional.of(UserPortfolioEntity.builder()
                        .userId("usr-1")
                        .totalCapital(new BigDecimal("12345"))
                        .build()));

        var series = adapter.listDashboardEquitySeries("usr-1", "7D");
        LocalDateTime nowAfter = LocalDateTime.now();

        assertEquals(2, series.size());
        assertRange(series.get(0).timestamp(), nowBefore.minusDays(7), nowAfter.minusDays(7));
        assertEquals(12345.0, series.get(0).value());
        assertRange(series.get(1).timestamp(), nowBefore, nowAfter);
        assertEquals(12345.0, series.get(1).value());
    }

    @Test
    void listDashboardEquitySeries_withSingleHistoricalPoint_prependsRangeAndAppendsSyntheticNow() {
        LocalDateTime historicalSnapshot = LocalDateTime.now().minusDays(2);
        LocalDateTime nowBefore = LocalDateTime.now();
        when(springDataPortfolioAggregateHistoryRepository.findByUserIdAndSnapshotAtAfterOrderBySnapshotAtAsc(eq("usr-1"), any()))
                .thenReturn(List.of(
                        PortfolioAggregateHistoryEntity.builder()
                                .snapshotAt(historicalSnapshot)
                                .total(new BigDecimal("10000"))
                                .build()
                ));
        when(springDataUserPortfolioRepository.findByUserId("usr-1"))
                .thenReturn(Optional.of(UserPortfolioEntity.builder()
                        .userId("usr-1")
                        .totalCapital(new BigDecimal("12000"))
                        .build()));

        var series = adapter.listDashboardEquitySeries("usr-1", "7D");
        LocalDateTime nowAfter = LocalDateTime.now();

        assertEquals(3, series.size());
        assertRange(series.get(0).timestamp(), nowBefore.minusDays(7), nowAfter.minusDays(7));
        assertEquals(10000.0, series.get(0).value());
        assertEquals(historicalSnapshot, series.get(1).timestamp());
        assertEquals(10000.0, series.get(1).value());
        assertRange(series.get(2).timestamp(), nowBefore, nowAfter);
        assertEquals(12000.0, series.get(2).value());
    }

    @Test
    void listDashboardEquitySeries_doesNotAppendDuplicateWhenLatestPointAlreadyExtendsPastNow() {
        LocalDateTime nowBefore = LocalDateTime.now();
        LocalDateTime futurePoint = nowBefore.plusMinutes(1);
        when(springDataPortfolioAggregateHistoryRepository.findByUserIdAndSnapshotAtAfterOrderBySnapshotAtAsc(eq("usr-1"), any()))
                .thenReturn(List.of(
                        PortfolioAggregateHistoryEntity.builder()
                                .snapshotAt(nowBefore.minusHours(2))
                                .total(new BigDecimal("10000"))
                                .build(),
                        PortfolioAggregateHistoryEntity.builder()
                                .snapshotAt(futurePoint)
                                .total(new BigDecimal("12000"))
                                .build()
                ));
        when(springDataUserPortfolioRepository.findByUserId("usr-1"))
                .thenReturn(Optional.of(UserPortfolioEntity.builder()
                        .userId("usr-1")
                        .totalCapital(new BigDecimal("12000"))
                        .build()));

        var series = adapter.listDashboardEquitySeries("usr-1", "1D");

        assertEquals(3, series.size());
        assertTrue(series.get(series.size() - 1).timestamp().isAfter(nowBefore));
        assertEquals(futurePoint, series.get(series.size() - 1).timestamp());
        assertEquals(12000.0, series.get(series.size() - 1).value());
    }

    @Test
    void listDashboardEquitySeries_resamplesDenseMonthlyHistoryAndKeepsExtremes() {
        LocalDateTime nowBefore = LocalDateTime.now();
        LocalDateTime firstHistoryPoint = nowBefore.minusDays(12);
        List<PortfolioAggregateHistoryEntity> history = new ArrayList<>();

        for (int i = 0; i < 96; i++) {
            double total = 10000 + i;
            if (i == 20) {
                total = 15000;
            } else if (i == 21) {
                total = 9000;
            }

            history.add(PortfolioAggregateHistoryEntity.builder()
                    .snapshotAt(firstHistoryPoint.plusHours(i * 3L))
                    .total(BigDecimal.valueOf(total))
                    .build());
        }

        when(springDataPortfolioAggregateHistoryRepository.findByUserIdAndSnapshotAtAfterOrderBySnapshotAtAsc(eq("usr-1"), any()))
                .thenReturn(history);
        when(springDataUserPortfolioRepository.findByUserId("usr-1"))
                .thenReturn(Optional.of(UserPortfolioEntity.builder()
                        .userId("usr-1")
                        .totalCapital(new BigDecimal("11111"))
                        .build()));

        var series = adapter.listDashboardEquitySeries("usr-1", "1M");
        LocalDateTime nowAfter = LocalDateTime.now();

        assertTrue(series.size() < history.size(), "Expected dense raw history to be bucketed for monthly timeframe");
        assertRange(series.get(0).timestamp(), nowBefore.minusDays(30), nowAfter.minusDays(30));
        assertRange(series.get(series.size() - 1).timestamp(), nowBefore, nowAfter);
        assertTrue(series.stream().anyMatch(point -> point.value() == 15000.0), "Expected resampling to preserve peak");
        assertTrue(series.stream().anyMatch(point -> point.value() == 9000.0), "Expected resampling to preserve drawdown");
    }

    @Test
    void listDashboardEquitySeries_allRangeResamplingDoesNotIntroduceZeroValues() {
        LocalDateTime nowBefore = LocalDateTime.now();
        LocalDateTime firstHistoryPoint = nowBefore.minusDays(220);
        List<PortfolioAggregateHistoryEntity> history = new ArrayList<>();

        for (int i = 0; i < 120; i++) {
            LocalDateTime timestamp = firstHistoryPoint.plusHours(i * 36L);
            double total = 10000 + (i * 17);
            if (i == 18) {
                total = 14800;
            } else if (i == 19) {
                total = 9100;
            } else if (i == 40) {
                total = 15125;
            } else if (i == 41) {
                total = 9250;
            }

            history.add(PortfolioAggregateHistoryEntity.builder()
                    .snapshotAt(timestamp)
                    .total(BigDecimal.valueOf(total))
                    .build());
        }

        history.add(PortfolioAggregateHistoryEntity.builder()
                .snapshotAt(firstHistoryPoint.plusDays(60))
                .total(BigDecimal.valueOf(11999))
                .build());
        history.add(PortfolioAggregateHistoryEntity.builder()
                .snapshotAt(firstHistoryPoint.plusDays(60))
                .total(BigDecimal.valueOf(12654))
                .build());

        when(springDataPortfolioAggregateHistoryRepository.findByUserIdAndSnapshotAtAfterOrderBySnapshotAtAsc(eq("usr-1"), any()))
                .thenReturn(history);
        when(springDataUserPortfolioRepository.findByUserId("usr-1"))
                .thenReturn(Optional.of(UserPortfolioEntity.builder()
                        .userId("usr-1")
                        .totalCapital(new BigDecimal("13333"))
                        .build()));

        var series = adapter.listDashboardEquitySeries("usr-1", "ALL");
        LocalDateTime nowAfter = LocalDateTime.now();

        assertTrue(series.size() < history.size(), "Expected ALL range to be bucketed");
        assertRange(series.get(0).timestamp(), nowBefore.minusYears(10), nowAfter.minusYears(10));
        assertRange(series.get(series.size() - 1).timestamp(), nowBefore, nowAfter);
        assertEquals(13333.0, series.get(series.size() - 1).value());
        assertTrue(series.stream().noneMatch(point -> point.value() == 0.0), "Unexpected zero value introduced during ALL resampling");
        assertTrue(series.stream().mapToDouble(point -> point.value()).max().orElseThrow() >= 14800.0,
                "Expected ALL resampling to preserve a major peak");
        assertTrue(series.stream().mapToDouble(point -> point.value()).min().orElseThrow() <= 9100.0,
                "Expected ALL resampling to preserve a major drawdown");
        assertEquals(12654.0, series.stream()
                .filter(point -> point.timestamp().equals(firstHistoryPoint.plusDays(60)))
                .reduce((first, second) -> second)
                .orElseThrow()
                .value());
    }

    @Test
    void listDashboardEquitySeries_allRangeDuplicateTimestampKeepsLastSourceValue() {
        LocalDateTime nowBefore = LocalDateTime.now();
        LocalDateTime duplicateTimestamp = nowBefore.minusDays(45);
        when(springDataPortfolioAggregateHistoryRepository.findByUserIdAndSnapshotAtAfterOrderBySnapshotAtAsc(eq("usr-1"), any()))
                .thenReturn(List.of(
                        PortfolioAggregateHistoryEntity.builder()
                                .snapshotAt(nowBefore.minusDays(120))
                                .total(new BigDecimal("10000"))
                                .build(),
                        PortfolioAggregateHistoryEntity.builder()
                                .snapshotAt(duplicateTimestamp)
                                .total(new BigDecimal("11888"))
                                .build(),
                        PortfolioAggregateHistoryEntity.builder()
                                .snapshotAt(duplicateTimestamp)
                                .total(new BigDecimal("12654"))
                                .build()
                ));
        when(springDataUserPortfolioRepository.findByUserId("usr-1"))
                .thenReturn(Optional.of(UserPortfolioEntity.builder()
                        .userId("usr-1")
                        .totalCapital(new BigDecimal("12777"))
                        .build()));

        var series = adapter.listDashboardEquitySeries("usr-1", "ALL");

        assertEquals(12777.0, series.get(series.size() - 1).value());
        assertEquals(12654.0, series.stream()
                .filter(point -> point.timestamp().equals(duplicateTimestamp))
                .reduce((first, second) -> second)
                .orElseThrow()
                .value());
    }

    @Test
    void getSubscriptionDecisions_filtersActiveAndAtRiskPortfolioViews() {
        UserPortfolioEntity portfolio = UserPortfolioEntity.builder()
                .userId("usr-1")
                .totalCapital(new BigDecimal("10000"))
                .availableBalance(new BigDecimal("8200"))
                .unrealizedPnl(new BigDecimal("275"))
                .maxDrawdownThreshold(new BigDecimal("0.1000"))
                .mediumRiskThreshold(new BigDecimal("0.0500"))
                .build();

        List<UserSubscriptionEntity> subscriptions = List.of(
                subscription("sub-solid", "usr-1", "bot-solid"),
                subscription("sub-slip", "usr-1", "bot-slip"),
                subscription("sub-review", "usr-1", "bot-review"),
                subscription("sub-risk", "usr-1", "bot-risk")
        );

        when(springDataUserSubscriptionRepository.findByUserIdAndStatus("usr-1", SubscriptionStatus.ACTIVE))
                .thenReturn(subscriptions);
        when(springDataUserPortfolioRepository.findByUserId("usr-1")).thenReturn(Optional.of(portfolio));

        stubDecisionBot("bot-solid", "SOL Trend", "OKX", List.of(signal("bot-solid", 1, 100, 110, 95)));
        stubDecisionBot("bot-slip", "ADA Drift", "BINANCE", List.of());
        stubDecisionBot("bot-review", "ETH Momentum", "BYBIT", List.of(signal("bot-review", 1, 100, 92, 95)));
        stubDecisionBot("bot-risk", "BTC Sentinel", "BINANCE", List.of(signal("bot-risk", 1, 100, 85, 90)));

        stubTelemetry("sub-solid",
                portfolioAccount("sub-solid", "bot-solid", 10600, 0, LocalDateTime.now().minusHours(1)),
                balanceHistory("usr-1", "sub-solid", "bot-solid", 10000, LocalDateTime.now().minusDays(3)),
                balanceHistory("usr-1", "sub-solid", "bot-solid", 10400, LocalDateTime.now().minusDays(2)),
                balanceHistory("usr-1", "sub-solid", "bot-solid", 10600, LocalDateTime.now().minusHours(2))
        );
        stubTelemetry("sub-slip",
                portfolioAccount("sub-slip", "bot-slip", 10100, 0, LocalDateTime.now().minusHours(1)),
                balanceHistory("usr-1", "sub-slip", "bot-slip", 10000, LocalDateTime.now().minusDays(2)),
                balanceHistory("usr-1", "sub-slip", "bot-slip", 10100, LocalDateTime.now().minusHours(3))
        );
        stubTelemetry("sub-review",
                portfolioAccount("sub-review", "bot-review", 9600, 0, LocalDateTime.now().minusHours(1)),
                balanceHistory("usr-1", "sub-review", "bot-review", 10000, LocalDateTime.now().minusDays(4)),
                balanceHistory("usr-1", "sub-review", "bot-review", 9400, LocalDateTime.now().minusDays(2)),
                balanceHistory("usr-1", "sub-review", "bot-review", 9600, LocalDateTime.now().minusHours(3))
        );
        stubTelemetry("sub-risk",
                portfolioAccount("sub-risk", "bot-risk", 8500, 0, LocalDateTime.now().minusHours(1)),
                balanceHistory("usr-1", "sub-risk", "bot-risk", 10000, LocalDateTime.now().minusDays(3)),
                balanceHistory("usr-1", "sub-risk", "bot-risk", 8500, LocalDateTime.now().minusHours(2))
        );

        List<SubscriptionDecisionSnapshot> all = adapter.getSubscriptionDecisions("usr-1", "ALL");
        List<SubscriptionDecisionSnapshot> active = adapter.getSubscriptionDecisions("usr-1", "ACTIVE");
        List<SubscriptionDecisionSnapshot> atRisk = adapter.getSubscriptionDecisions("usr-1", "AT_RISK");

        assertEquals(4, all.size());
        assertEquals(List.of("bot-slip", "bot-solid"), active.stream().map(SubscriptionDecisionSnapshot::botId).sorted().toList());
        assertEquals(List.of("bot-review", "bot-risk"), atRisk.stream().map(SubscriptionDecisionSnapshot::botId).sorted().toList());
        verify(springDataUserSubscriptionRepository, times(3)).findByUserIdAndStatus("usr-1", SubscriptionStatus.ACTIVE);
    }

    @Test
    void getSubscriptionDecisions_usesSubscriptionTelemetryForPnlAndDrawdown() {
        UserPortfolioEntity portfolio = UserPortfolioEntity.builder()
                .userId("usr-1")
                .totalCapital(new BigDecimal("10000"))
                .availableBalance(new BigDecimal("7000"))
                .unrealizedPnl(new BigDecimal("150"))
                .maxDrawdownThreshold(new BigDecimal("0.1000"))
                .mediumRiskThreshold(new BigDecimal("0.0500"))
                .build();
        UserSubscriptionEntity subscription = subscription("sub-telemetry", "usr-1", "bot-telemetry");
        when(springDataUserSubscriptionRepository.findByUserIdAndStatus("usr-1", SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(subscription));
        when(springDataUserPortfolioRepository.findByUserId("usr-1")).thenReturn(Optional.of(portfolio));

        stubDecisionBot("bot-telemetry", "Telemetry Bot", "BINANCE", List.of(signal("bot-telemetry", 1, 100, 110, 95)));
        stubTelemetry("sub-telemetry",
                portfolioAccount("sub-telemetry", "bot-telemetry", 10500, 0, LocalDateTime.now().minusHours(1)),
                balanceHistory("usr-1", "sub-telemetry", "bot-telemetry", 10000, LocalDateTime.now().minusDays(3)),
                balanceHistory("usr-1", "sub-telemetry", "bot-telemetry", 11000, LocalDateTime.now().minusDays(2)),
                balanceHistory("usr-1", "sub-telemetry", "bot-telemetry", 9000, LocalDateTime.now().minusDays(1)),
                balanceHistory("usr-1", "sub-telemetry", "bot-telemetry", 10200, LocalDateTime.now().minusHours(6))
        );

        SubscriptionDecisionSnapshot snapshot = adapter.getSubscriptionDecisions("usr-1", "ALL").get(0);

        assertEquals(500.0, snapshot.currentPnL(), 0.0001);
        assertEquals(0.05, snapshot.pnlPercent(), 0.0001);
        assertEquals(-0.1818, snapshot.drawdownPercent(), 0.0001);
        assertEquals("FRESH", snapshot.syncFreshness());
        assertNotNull(snapshot.lastSyncAt());
    }

    @Test
    void getSubscriptionDecisions_withoutTelemetry_marksSubscriptionUnsyncedForReview() {
        UserPortfolioEntity portfolio = UserPortfolioEntity.builder()
                .userId("usr-1")
                .totalCapital(new BigDecimal("10000"))
                .availableBalance(new BigDecimal("10000"))
                .unrealizedPnl(BigDecimal.ZERO)
                .maxDrawdownThreshold(new BigDecimal("0.1000"))
                .mediumRiskThreshold(new BigDecimal("0.0500"))
                .build();
        UserSubscriptionEntity subscription = subscription("sub-empty", "usr-1", "bot-empty");
        when(springDataUserSubscriptionRepository.findByUserIdAndStatus("usr-1", SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(subscription));
        when(springDataUserPortfolioRepository.findByUserId("usr-1")).thenReturn(Optional.of(portfolio));

        stubDecisionBot("bot-empty", "Empty Bot", "OKX", List.of());
        stubTelemetry("sub-empty", null);

        SubscriptionDecisionSnapshot snapshot = adapter.getSubscriptionDecisions("usr-1", "ALL").get(0);

        assertNull(snapshot.currentPnL());
        assertNull(snapshot.pnlPercent());
        assertNull(snapshot.drawdownPercent());
        assertEquals(PortfolioReadPort.DecisionReason.NEEDS_REVIEW, snapshot.reason());
        assertEquals("NEVER_SYNCED", snapshot.syncFreshness());
        assertTrue(snapshot.reasonExplanation().contains("No subscription telemetry"));
    }

    @Test
    void getSubscriptionDecisions_withStaleSync_marksNeedsReview() {
        UserPortfolioEntity portfolio = UserPortfolioEntity.builder()
                .userId("usr-1")
                .totalCapital(new BigDecimal("10000"))
                .availableBalance(new BigDecimal("9500"))
                .unrealizedPnl(new BigDecimal("50"))
                .maxDrawdownThreshold(new BigDecimal("0.1000"))
                .mediumRiskThreshold(new BigDecimal("0.0500"))
                .build();
        UserSubscriptionEntity subscription = subscription("sub-stale", "usr-1", "bot-stale");
        when(springDataUserSubscriptionRepository.findByUserIdAndStatus("usr-1", SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(subscription));
        when(springDataUserPortfolioRepository.findByUserId("usr-1")).thenReturn(Optional.of(portfolio));

        stubDecisionBot("bot-stale", "Stale Bot", "BYBIT", List.of(signal("bot-stale", 1, 100, 110, 95)));
        stubTelemetry("sub-stale",
                portfolioAccount("sub-stale", "bot-stale", 10100, 0, LocalDateTime.now().minusHours(30)),
                balanceHistory("usr-1", "sub-stale", "bot-stale", 10000, LocalDateTime.now().minusDays(4)),
                balanceHistory("usr-1", "sub-stale", "bot-stale", 10100, LocalDateTime.now().minusHours(30))
        );

        SubscriptionDecisionSnapshot snapshot = adapter.getSubscriptionDecisions("usr-1", "ALL").get(0);

        assertEquals(PortfolioReadPort.DecisionReason.NEEDS_REVIEW, snapshot.reason());
        assertEquals("STALE", snapshot.syncFreshness());
        assertTrue(snapshot.reasonExplanation().contains("stale"));
    }

    @Test
    void getSubscriptionDecisions_withFreshSyncButNoRecentSignals_marksSlipping() {
        UserPortfolioEntity portfolio = UserPortfolioEntity.builder()
                .userId("usr-1")
                .totalCapital(new BigDecimal("10000"))
                .availableBalance(new BigDecimal("9400"))
                .unrealizedPnl(new BigDecimal("20"))
                .maxDrawdownThreshold(new BigDecimal("0.1000"))
                .mediumRiskThreshold(new BigDecimal("0.0500"))
                .build();
        UserSubscriptionEntity subscription = subscription("sub-slip", "usr-1", "bot-slip");
        when(springDataUserSubscriptionRepository.findByUserIdAndStatus("usr-1", SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(subscription));
        when(springDataUserPortfolioRepository.findByUserId("usr-1")).thenReturn(Optional.of(portfolio));

        stubDecisionBot("bot-slip", "Slip Bot", "BINANCE", List.of());
        stubTelemetry("sub-slip",
                portfolioAccount("sub-slip", "bot-slip", 10150, 0, LocalDateTime.now().minusHours(1)),
                balanceHistory("usr-1", "sub-slip", "bot-slip", 10000, LocalDateTime.now().minusDays(4)),
                balanceHistory("usr-1", "sub-slip", "bot-slip", 10150, LocalDateTime.now().minusHours(3))
        );

        SubscriptionDecisionSnapshot snapshot = adapter.getSubscriptionDecisions("usr-1", "ALL").get(0);

        assertEquals(PortfolioReadPort.DecisionReason.SLIPPING, snapshot.reason());
        assertEquals("FRESH", snapshot.syncFreshness());
        assertTrue(snapshot.reasonExplanation().contains("last 24 hours"));
    }

    @Test
    void getPortfolioOverview_usesRealUnrealizedPnlInsteadOfSignalDerivedSum() {
        UserPortfolioEntity portfolio = UserPortfolioEntity.builder()
                .userId("usr-1")
                .totalCapital(new BigDecimal("15000"))
                .availableBalance(new BigDecimal("12000"))
                .unrealizedPnl(new BigDecimal("321.5"))
                .maxDrawdownThreshold(new BigDecimal("0.1000"))
                .mediumRiskThreshold(new BigDecimal("0.0500"))
                .freshAccountsCount(1)
                .staleAccountsCount(1)
                .dataFreshness("PARTIAL")
                .lastSyncAt(LocalDateTime.now().minusMinutes(5))
                .build();
        List<UserSubscriptionEntity> subscriptions = List.of(
                subscription("sub-1", "usr-1", "bot-1"),
                subscription("sub-2", "usr-1", "bot-2")
        );
        when(springDataUserSubscriptionRepository.findByUserIdAndStatus("usr-1", SubscriptionStatus.ACTIVE))
                .thenReturn(subscriptions);
        when(springDataUserPortfolioRepository.findByUserId("usr-1")).thenReturn(Optional.of(portfolio));

        stubDecisionBot("bot-1", "Bot 1", "BINANCE", List.of(signal("bot-1", 1, 100, 110, 95)));
        stubDecisionBot("bot-2", "Bot 2", "OKX", List.of());
        stubTelemetry("sub-1",
                portfolioAccount("sub-1", "bot-1", 10500, 0, LocalDateTime.now().minusHours(1)),
                balanceHistory("usr-1", "sub-1", "bot-1", 10000, LocalDateTime.now().minusDays(2)),
                balanceHistory("usr-1", "sub-1", "bot-1", 10500, LocalDateTime.now().minusHours(2))
        );
        stubTelemetry("sub-2",
                portfolioAccount("sub-2", "bot-2", 9800, 0, LocalDateTime.now().minusHours(30)),
                balanceHistory("usr-1", "sub-2", "bot-2", 10000, LocalDateTime.now().minusDays(2)),
                balanceHistory("usr-1", "sub-2", "bot-2", 9800, LocalDateTime.now().minusHours(30))
        );

        PortfolioReadPort.PortfolioOverviewSnapshot snapshot = adapter.getPortfolioOverview("usr-1");

        assertEquals(321.5, snapshot.aggregateOpenPnL(), 0.0001);
        assertEquals(1, snapshot.atRiskSubscriptionCount());
        assertEquals("PARTIAL", snapshot.dataFreshness());
    }

    private void assertRange(LocalDateTime actual, LocalDateTime minInclusive, LocalDateTime maxInclusive) {
        assertFalse(actual.isBefore(minInclusive), "Expected timestamp >= " + minInclusive + " but was " + actual);
        assertFalse(actual.isAfter(maxInclusive), "Expected timestamp <= " + maxInclusive + " but was " + actual);
    }

    private void stubDecisionBot(String botId, String name, String exchangeId, List<SignalEntity> signals) {
        when(springDataBotRepository.findByBotId(botId)).thenReturn(Optional.of(
                BotEntity.builder()
                        .botId(botId)
                        .name(name)
                        .developerId("dev-1")
                        .status(BotStatus.ACTIVE)
                        .tradingPair("BTCUSDT")
                        .exchange(ExchangeEntity.builder().exchangeId(exchangeId).name(exchangeId).build())
                        .build()
        ));
        when(springDataSignalRepository.findByBotIdAndCreatedAtAfter(eq(botId), any(LocalDateTime.class)))
                .thenReturn(signals);
        when(springDataSignalRepository.findByBotIdOrderByGeneratedTimestampDesc(eq(botId), any()))
                .thenReturn(signals.stream()
                        .sorted((a, b) -> {
                            LocalDateTime aTs = a.getGeneratedTimestamp();
                            LocalDateTime bTs = b.getGeneratedTimestamp();
                            if (aTs == null && bTs == null) return 0;
                            if (aTs == null) return 1;
                            if (bTs == null) return -1;
                            return bTs.compareTo(aTs);
                        })
                        .toList());
    }

    private void stubTelemetry(String subscriptionId, PortfolioAccountEntity account, PortfolioBalanceHistoryEntity... history) {
        when(springDataPortfolioAccountRepository.findTopByUserSubscriptionIdOrderByLastSyncAtDesc(subscriptionId))
                .thenReturn(Optional.ofNullable(account));
        when(springDataPortfolioHistoryRepository.findByUserSubscriptionIdAndSnapshotAtAfterOrderBySnapshotAtAsc(eq(subscriptionId), any(LocalDateTime.class)))
                .thenReturn(List.of(history));
        lenient().when(springDataPortfolioHistoryRepository.findByUserSubscriptionIdOrderBySnapshotAtAsc(subscriptionId))
                .thenReturn(List.of(history));
    }

    private PortfolioAccountEntity portfolioAccount(String subscriptionId, String botId, double total, double unrealizedPnl, LocalDateTime lastSyncAt) {
        return PortfolioAccountEntity.builder()
                .userId("usr-1")
                .userSubscriptionId(subscriptionId)
                .botId(botId)
                .total(BigDecimal.valueOf(total))
                .free(BigDecimal.valueOf(total))
                .used(BigDecimal.ZERO)
                .realizedPnl(BigDecimal.ZERO)
                .unrealizedPnl(BigDecimal.valueOf(unrealizedPnl))
                .lastSyncAt(lastSyncAt)
                .active(true)
                .build();
    }

    private PortfolioBalanceHistoryEntity balanceHistory(
            String userId,
            String subscriptionId,
            String botId,
            double total,
            LocalDateTime snapshotAt
    ) {
        return PortfolioBalanceHistoryEntity.builder()
                .userId(userId)
                .userSubscriptionId(subscriptionId)
                .botId(botId)
                .total(BigDecimal.valueOf(total))
                .free(BigDecimal.valueOf(total))
                .used(BigDecimal.ZERO)
                .unrealizedPnl(BigDecimal.ZERO)
                .active(true)
                .snapshotAt(snapshotAt)
                .build();
    }

    private UserSubscriptionEntity subscription(String id, String userId, String botId) {
        return UserSubscriptionEntity.builder()
                .id(id)
                .userSubscriptionId(id)
                .userId(userId)
                .botId(botId)
                .wsToken("ws-" + botId)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDateTime.now().minusDays(10))
                .build();
    }

    private SignalEntity signal(String botId, long hoursAgo, double entry, double takeProfit, double stopLoss) {
        LocalDateTime generatedAt = LocalDateTime.now().minusHours(hoursAgo);
        return SignalEntity.builder()
                .signalId(botId + "-" + hoursAgo)
                .botId(botId)
                .symbol("BTCUSDT")
                .action(SignalAction.OPEN_LONG)
                .marketType(MarketType.SPOT)
                .orderType(OrderType.LIMIT)
                .entry(BigDecimal.valueOf(entry))
                .takeProfit(BigDecimal.valueOf(takeProfit))
                .stopLoss(BigDecimal.valueOf(stopLoss))
                .status(SignalStatus.ACKNOWLEDGED)
                .generatedTimestamp(generatedAt)
                .build();
    }
}
