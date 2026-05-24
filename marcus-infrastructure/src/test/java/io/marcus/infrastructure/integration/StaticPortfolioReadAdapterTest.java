package io.marcus.infrastructure.integration;

import io.marcus.domain.port.PortfolioReadPort.ExecutionLogPageSnapshot;
import io.marcus.domain.port.PortfolioReadPort.ExecutionLogItemSnapshot;
import io.marcus.domain.port.PortfolioReadPort.BotIntegrationHealthSnapshot;
import io.marcus.domain.port.PortfolioReadPort.ConnectivityHealthDependencySnapshot;
import io.marcus.infrastructure.persistence.entity.BotEntity;
import io.marcus.infrastructure.persistence.entity.SignalEntity;
import io.marcus.infrastructure.persistence.SpringDataBotRepository;
import io.marcus.infrastructure.persistence.SpringDataRawEventRepository;
import io.marcus.infrastructure.persistence.SpringDataSignalRepository;
import io.marcus.infrastructure.persistence.SpringDataUserPortfolioRepository;
import io.marcus.infrastructure.persistence.SpringDataUserSubscriptionRepository;
import io.marcus.infrastructure.persistence.entity.RawEventEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import java.util.NoSuchElementException;
import java.time.LocalDateTime;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaticPortfolioReadAdapterTest {

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

    private StaticPortfolioReadAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new StaticPortfolioReadAdapter(
                springDataBotRepository,
                springDataSignalRepository,
                springDataUserSubscriptionRepository,
                springDataUserPortfolioRepository,
                springDataRawEventRepository
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
        
        ConnectivityHealthDependencySnapshot wsDep = snapshot.dependencies().stream()
                .filter(d -> "WebSocket Stream".equals(d.name())).findFirst().orElseThrow();
        assertEquals("UP", wsDep.status());
        assertEquals(15, wsDep.latencyMs());

        ConnectivityHealthDependencySnapshot sigDep = snapshot.dependencies().stream()
                .filter(d -> "Signal Processor".equals(d.name())).findFirst().orElseThrow();
        assertEquals("UP", sigDep.status());
    }

    @Test
    void getBotIntegrationHealth_degradedHeartbeat_returnsDegraded() {
        String botId = "bot-123";
        BotEntity bot = BotEntity.builder().botId(botId).build();
        when(springDataBotRepository.findByBotId(botId)).thenReturn(Optional.of(bot));

        // Heartbeat is somewhat old (2 minutes / 120 seconds ago)
        RawEventEntity heartbeat = RawEventEntity.builder()
                .botId(botId)
                .type("heartbeat")
                .receivedAt(Instant.now().minusSeconds(120))
                .build();
        when(springDataRawEventRepository.findLatestHeartbeatForBot(botId)).thenReturn(Optional.of(heartbeat));
        when(springDataSignalRepository.findByBotId(botId)).thenReturn(List.of());

        BotIntegrationHealthSnapshot snapshot = adapter.getBotIntegrationHealth(botId);

        assertNotNull(snapshot);
        assertEquals("DEGRADED", snapshot.overallStatus());
        assertTrue(snapshot.message().contains("degraded"));

        ConnectivityHealthDependencySnapshot wsDep = snapshot.dependencies().stream()
                .filter(d -> "WebSocket Stream".equals(d.name())).findFirst().orElseThrow();
        assertEquals("DEGRADED", wsDep.status());
        assertEquals(45, wsDep.latencyMs());
    }

    @Test
    void getBotIntegrationHealth_offlineHeartbeat_returnsDown() {
        String botId = "bot-123";
        BotEntity bot = BotEntity.builder().botId(botId).build();
        when(springDataBotRepository.findByBotId(botId)).thenReturn(Optional.of(bot));

        // Heartbeat is very old (10 minutes / 600 seconds ago)
        RawEventEntity heartbeat = RawEventEntity.builder()
                .botId(botId)
                .type("heartbeat")
                .receivedAt(Instant.now().minusSeconds(600))
                .build();
        when(springDataRawEventRepository.findLatestHeartbeatForBot(botId)).thenReturn(Optional.of(heartbeat));
        when(springDataSignalRepository.findByBotId(botId)).thenReturn(List.of());

        BotIntegrationHealthSnapshot snapshot = adapter.getBotIntegrationHealth(botId);

        assertNotNull(snapshot);
        assertEquals("DOWN", snapshot.overallStatus());
        assertTrue(snapshot.message().contains("offline"));

        ConnectivityHealthDependencySnapshot wsDep = snapshot.dependencies().stream()
                .filter(d -> "WebSocket Stream".equals(d.name())).findFirst().orElseThrow();
        assertEquals("DOWN", wsDep.status());
        assertEquals(999, wsDep.latencyMs());
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

        ConnectivityHealthDependencySnapshot wsDep = snapshot.dependencies().stream()
                .filter(d -> "WebSocket Stream".equals(d.name())).findFirst().orElseThrow();
        assertEquals("UP", wsDep.status());

        ConnectivityHealthDependencySnapshot sigDep = snapshot.dependencies().stream()
                .filter(d -> "Signal Processor".equals(d.name())).findFirst().orElseThrow();
        assertEquals("UP", sigDep.status());
    }
}
