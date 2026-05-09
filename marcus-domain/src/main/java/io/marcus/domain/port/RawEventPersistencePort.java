package io.marcus.domain.port;

import io.marcus.domain.model.RawEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Port for persisting and querying raw executor events.
 * Defines the contract for append-only raw event log storage.
 */
public interface RawEventPersistencePort {

    /**
     * Persist a raw event from the executor.
     * If an event with the same botId and idempotencyKey already exists,
     * returns the existing entity instead of creating a duplicate.
     *
     * @param rawEvent the raw event to persist
     * @return the persisted (or existing) raw event with assigned ID and sequence
     */
    RawEvent save(RawEvent rawEvent);

    /**
     * Find a raw event by its eventId.
     *
     * @param eventId unique event identifier
     * @return the raw event, or empty if not found
     */
    Optional<RawEvent> findByEventId(String eventId);

    /**
     * Check for duplicate: find by botId and idempotencyKey.
     * Used to detect retries and enforce idempotency.
     *
     * @param botId bot identifier
     * @param idempotencyKey idempotency key from the message envelope
     * @return the existing raw event if found
     */
    Optional<RawEvent> findExistingByIdempotencyKey(String botId, String idempotencyKey);

    /**
     * Fetch unprocessed raw events for a bot (for projection worker).
     *
     * @param botId bot identifier
     * @return list of unprocessed events ordered by sequence
     */
    List<RawEvent> findUnprocessedForBot(String botId);

    /**
     * Mark a raw event as processed.
     *
     * @param eventId event identifier
     * @param processedAt processing completion timestamp
     */
    void markProcessed(String eventId, Instant processedAt);

    /**
     * Fetch raw events within a sequence range (for replay).
     *
     * @param botId bot identifier
     * @param fromSeq sequence lower bound (inclusive)
     * @param toSeq sequence upper bound (inclusive)
     * @return list of raw events in the range, ordered by sequence
     */
    List<RawEvent> findBySequenceRange(String botId, Long fromSeq, Long toSeq);

    /**
     * Fetch raw events within a time range (for replay).
     *
     * @param botId bot identifier
     * @param from start time (inclusive)
     * @param to end time (inclusive)
     * @return list of raw events in the range, ordered by sequence
     */
    List<RawEvent> findByTimeRange(String botId, Instant from, Instant to);

    /**
     * Fetch raw events by correlationId (for debugging and audit).
     * Returns paginated results.
     *
     * @param correlationId correlation identifier
     * @param limit max results
     * @param offset pagination offset
     * @return list of raw events matching the correlationId
     */
    List<RawEvent> findByCorrelationId(String correlationId, int limit, int offset);

    /**
     * Get the next sequence number for a bot.
     * Increments atomically to avoid gaps.
     *
     * @param botId bot identifier
     * @return next sequence number to assign
     */
    Long getNextSequence(String botId);
}
