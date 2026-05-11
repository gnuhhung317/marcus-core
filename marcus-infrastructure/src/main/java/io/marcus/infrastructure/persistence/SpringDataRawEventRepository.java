package io.marcus.infrastructure.persistence;

import io.marcus.infrastructure.persistence.entity.RawEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for append-only raw event log. Ensures idempotency and
 * enables efficient replay queries.
 */
public interface SpringDataRawEventRepository extends JpaRepository<RawEventEntity, String> {

    /**
     * Find a raw event by its unique eventId.
     */
    Optional<RawEventEntity> findByEventId(String eventId);

    /**
     * Check if an event with this botId and idempotencyKey already exists.
     * Returns the entity if found (for deduplication).
     */
    Optional<RawEventEntity> findByBotIdAndIdempotencyKey(String botId, String idempotencyKey);

    /**
     * Find all unprocessed raw events for a bot, ordered by sequence. Used by
     * the projection worker to catch up.
     */
    List<RawEventEntity> findByBotIdAndProcessedFalseOrderBySequenceNoAsc(String botId);

    /**
     * Find raw events by botId and sequence range (inclusive). Used for
     * replay-request handling.
     */
    @Query(
            "SELECT r FROM RawEventEntity r "
            + "WHERE r.botId = :botId AND r.sequenceNo >= :fromSeq AND r.sequenceNo <= :toSeq "
            + "ORDER BY r.sequenceNo ASC"
    )
    List<RawEventEntity> findByBotIdAndSequenceRange(
            @Param("botId") String botId,
            @Param("fromSeq") Long fromSeq,
            @Param("toSeq") Long toSeq
    );

    /**
     * Find raw events by botId and time range. Used for replay-request with
     * timestamp-based filtering.
     */
    @Query(
            "SELECT r FROM RawEventEntity r "
            + "WHERE r.botId = :botId AND r.receivedAt >= :from AND r.receivedAt <= :to "
            + "ORDER BY r.sequenceNo ASC"
    )
    List<RawEventEntity> findByBotIdAndTimeRange(
            @Param("botId") String botId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    /**
     * Find raw events by correlationId (for debugging and audit trail). Results
     * are offset-paginated and ordered deterministically by sequence.
     */
    @Query(value = "SELECT * FROM raw_events "
            + "WHERE correlation_id = :correlationId "
            + "ORDER BY sequence_no ASC "
            + "OFFSET :offset LIMIT :limit", nativeQuery = true)
    List<RawEventEntity> findByCorrelationId(
            @Param("correlationId") String correlationId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * Get the maximum sequence number for a botId. Used to assign the next
     * sequence number on ingest.
     */
    @Query("SELECT MAX(r.sequenceNo) FROM RawEventEntity r WHERE r.botId = :botId")
    Optional<Long> getMaxSequenceForBot(@Param("botId") String botId);
}
