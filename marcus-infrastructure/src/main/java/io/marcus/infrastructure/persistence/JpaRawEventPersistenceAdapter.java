package io.marcus.infrastructure.persistence;

import io.marcus.domain.model.RawEvent;
import io.marcus.domain.port.RawEventPersistencePort;
import io.marcus.infrastructure.persistence.entity.RawEventEntity;
import io.marcus.infrastructure.persistence.mapper.RawEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * JPA-based implementation of RawEventPersistencePort. Handles idempotency,
 * sequence generation, and query operations for raw events.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class JpaRawEventPersistenceAdapter implements RawEventPersistencePort {

    private final SpringDataRawEventRepository repository;
    private final RawEventMapper mapper;

    @Override
    public RawEvent save(RawEvent rawEvent) {
        // Check for duplicate by botId and idempotencyKey
        Optional<RawEventEntity> existing = repository.findByBotIdAndIdempotencyKey(
                rawEvent.getBotId(),
                rawEvent.getIdempotencyKey()
        );

        if (existing.isPresent()) {
            // Idempotent: return the existing event without creating a duplicate
            return mapper.entityToDomain(existing.get());
        }

        // Assign next sequence number for this bot
        Long nextSeq = getNextSequence(rawEvent.getBotId());
        rawEvent.setSequenceNo(nextSeq);

        // Set default values for new event
        if (rawEvent.getReceivedAt() == null) {
            rawEvent.setReceivedAt(Instant.now());
        }
        if (rawEvent.getProcessed() == null) {
            rawEvent.setProcessed(false);
        }

        // Persist and return. Handle race on unique constraint (botId, idempotencyKey).
        RawEventEntity entity = mapper.domainToEntity(rawEvent);
        try {
            RawEventEntity saved = repository.save(entity);
            return mapper.entityToDomain(saved);
        } catch (DataIntegrityViolationException dive) {
            // Likely a concurrent insert caused unique constraint violation.
            log.warn("Unique constraint violation while saving RawEvent (bot={}, idempotency={}), attempting lookup: {}",
                    rawEvent.getBotId(), rawEvent.getIdempotencyKey(), dive.getMessage());

            Optional<RawEventEntity> existingAfterConflict = repository.findByBotIdAndIdempotencyKey(
                    rawEvent.getBotId(), rawEvent.getIdempotencyKey());

            if (existingAfterConflict.isPresent()) {
                return mapper.entityToDomain(existingAfterConflict.get());
            }

            // If not found, rethrow to surface the unexpected error
            throw dive;
        }
    }

    @Override
    public Optional<RawEvent> findByEventId(String eventId) {
        return repository.findByEventId(eventId)
                .map(mapper::entityToDomain);
    }

    @Override
    public Optional<RawEvent> findExistingByIdempotencyKey(String botId, String idempotencyKey) {
        return repository.findByBotIdAndIdempotencyKey(botId, idempotencyKey)
                .map(mapper::entityToDomain);
    }

    @Override
    public List<RawEvent> findUnprocessedForBot(String botId) {
        return repository.findByBotIdAndProcessedFalseOrderBySequenceNoAsc(botId)
                .stream()
                .map(mapper::entityToDomain)
                .toList();
    }

    @Override
    public void markProcessed(String eventId, Instant processedAt) {
        repository.findByEventId(eventId).ifPresent(entity -> {
            entity.setProcessed(true);
            entity.setProcessedAt(processedAt);
            repository.save(entity);
        });
    }

    @Override
    public List<RawEvent> findBySequenceRange(String botId, Long fromSeq, Long toSeq) {
        return repository.findByBotIdAndSequenceRange(botId, fromSeq, toSeq)
                .stream()
                .map(mapper::entityToDomain)
                .toList();
    }

    @Override
    public List<RawEvent> findByTimeRange(String botId, Instant from, Instant to) {
        return repository.findByBotIdAndTimeRange(botId, from, to)
                .stream()
                .map(mapper::entityToDomain)
                .toList();
    }

    @Override
    public List<RawEvent> findByCorrelationId(String correlationId, int limit, int offset) {
        return repository.findByCorrelationId(
                correlationId,
                PageRequest.of(offset / limit, limit)
        )
                .getContent()
                .stream()
                .map(mapper::entityToDomain)
                .toList();
    }

    @Override
    public Long getNextSequence(String botId) {
        Optional<Long> maxSeq = repository.getMaxSequenceForBot(botId);
        return maxSeq.map(seq -> seq + 1).orElse(1L);
    }
}
