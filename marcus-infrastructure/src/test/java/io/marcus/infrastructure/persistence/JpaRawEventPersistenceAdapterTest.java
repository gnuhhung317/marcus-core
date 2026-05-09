package io.marcus.infrastructure.persistence;

import io.marcus.domain.model.RawEvent;
import io.marcus.infrastructure.persistence.entity.RawEventEntity;
import io.marcus.infrastructure.persistence.mapper.RawEventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaRawEventPersistenceAdapterTest {

    @Mock
    private SpringDataRawEventRepository repository;

    @Mock
    private RawEventMapper mapper;

    private JpaRawEventPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaRawEventPersistenceAdapter(repository, mapper);
    }

    @Test
    void shouldReturnExistingRawEventWhenUniqueConstraintIsHit() {
        RawEvent request = RawEvent.builder()
                .botId("bot_1")
                .eventId("evt_1")
                .idempotencyKey("idem_1")
                .correlationId("corr_1")
                .type("ingest")
                .payload(Map.of("symbol", "BTCUSDT"))
                .sourceConnId("conn_1")
                .receivedAt(Instant.parse("2026-05-09T10:00:00Z"))
                .processed(false)
                .build();

        RawEventEntity attemptedEntity = RawEventEntity.builder()
                .botId("bot_1")
                .eventId("evt_1")
                .idempotencyKey("idem_1")
                .correlationId("corr_1")
                .type("ingest")
                .payload(Map.of("symbol", "BTCUSDT"))
                .sourceConnId("conn_1")
                .receivedAt(Instant.parse("2026-05-09T10:00:00Z"))
                .processed(false)
                .sequenceNo(1L)
                .build();

        RawEventEntity existingEntity = RawEventEntity.builder()
                .id("raw_1")
                .botId("bot_1")
                .eventId("evt_1")
                .idempotencyKey("idem_1")
                .correlationId("corr_1")
                .type("ingest")
                .payload(Map.of("symbol", "BTCUSDT"))
                .sourceConnId("conn_1")
                .receivedAt(Instant.parse("2026-05-09T10:00:00Z"))
                .processed(false)
                .sequenceNo(1L)
                .build();

        RawEvent expected = RawEvent.builder()
                .id("raw_1")
                .botId("bot_1")
                .eventId("evt_1")
                .idempotencyKey("idem_1")
                .correlationId("corr_1")
                .type("ingest")
                .payload(Map.of("symbol", "BTCUSDT"))
                .sourceConnId("conn_1")
                .receivedAt(Instant.parse("2026-05-09T10:00:00Z"))
                .processed(false)
                .sequenceNo(1L)
                .build();

        when(repository.findByBotIdAndIdempotencyKey("bot_1", "idem_1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingEntity));
        when(repository.getMaxSequenceForBot("bot_1")).thenReturn(Optional.of(0L));
        when(mapper.domainToEntity(any(RawEvent.class))).thenReturn(attemptedEntity);
        when(repository.save(attemptedEntity)).thenThrow(new DataIntegrityViolationException("duplicate key"));
        when(mapper.entityToDomain(existingEntity)).thenReturn(expected);

        RawEvent actual = adapter.save(request);

        assertThat(actual).isEqualTo(expected);
        verify(repository).save(attemptedEntity);
                verify(repository, atLeast(2)).findByBotIdAndIdempotencyKey("bot_1", "idem_1");
    }

    @Test
    void shouldPersistNewRawEventNormally() {
        RawEvent request = RawEvent.builder()
                .botId("bot_2")
                .eventId("evt_2")
                .idempotencyKey("idem_2")
                .correlationId("corr_2")
                .type("ingest")
                .payload(Map.of("symbol", "ETHUSDT"))
                .sourceConnId("conn_2")
                .receivedAt(Instant.parse("2026-05-09T10:10:00Z"))
                .processed(false)
                .build();

        RawEventEntity mappedEntity = RawEventEntity.builder()
                .botId("bot_2")
                .eventId("evt_2")
                .idempotencyKey("idem_2")
                .correlationId("corr_2")
                .type("ingest")
                .payload(Map.of("symbol", "ETHUSDT"))
                .sourceConnId("conn_2")
                .receivedAt(Instant.parse("2026-05-09T10:10:00Z"))
                .processed(false)
                .sequenceNo(1L)
                .build();

        RawEventEntity savedEntity = RawEventEntity.builder()
                .id("raw_2")
                .botId("bot_2")
                .eventId("evt_2")
                .idempotencyKey("idem_2")
                .correlationId("corr_2")
                .type("ingest")
                .payload(Map.of("symbol", "ETHUSDT"))
                .sourceConnId("conn_2")
                .receivedAt(Instant.parse("2026-05-09T10:10:00Z"))
                .processed(false)
                .sequenceNo(1L)
                .build();

        RawEvent expected = RawEvent.builder()
                .id("raw_2")
                .botId("bot_2")
                .eventId("evt_2")
                .idempotencyKey("idem_2")
                .correlationId("corr_2")
                .type("ingest")
                .payload(Map.of("symbol", "ETHUSDT"))
                .sourceConnId("conn_2")
                .receivedAt(Instant.parse("2026-05-09T10:10:00Z"))
                .processed(false)
                .sequenceNo(1L)
                .build();

        when(repository.findByBotIdAndIdempotencyKey("bot_2", "idem_2")).thenReturn(Optional.empty());
        when(repository.getMaxSequenceForBot("bot_2")).thenReturn(Optional.of(0L));
        when(mapper.domainToEntity(any(RawEvent.class))).thenReturn(mappedEntity);
        when(repository.save(mappedEntity)).thenReturn(savedEntity);
        when(mapper.entityToDomain(savedEntity)).thenReturn(expected);

        RawEvent actual = adapter.save(request);

        assertThat(actual).isEqualTo(expected);
        verify(repository).save(mappedEntity);
    }
}
