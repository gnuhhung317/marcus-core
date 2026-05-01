package io.marcus.infrastructure.persistence.executor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for ExecutionEventEntity.
 * Provides basic CRUD and custom query operations.
 */
@Repository
public interface ExecutionEventRepository extends JpaRepository<ExecutionEventEntity, Long> {

    /**
     * Find all events for a signal in sequence order.
     */
    List<ExecutionEventEntity> findBySignalIdOrderBySequenceAsc(String signalId);

    /**
     * Find events within sequence range.
     */
    @Query("SELECT e FROM ExecutionEventEntity e WHERE e.signalId = :signalId AND e.sequence >= :fromSequence AND e.sequence <= :toSequence ORDER BY e.sequence ASC")
    List<ExecutionEventEntity> findBySignalIdAndSequenceRange(
            @Param("signalId") String signalId,
            @Param("fromSequence") int fromSequence,
            @Param("toSequence") int toSequence
    );

    /**
     * Check if event exists by eventId.
     */
    boolean existsByEventId(String eventId);

    /**
     * Find event by eventId.
     */
    Optional<ExecutionEventEntity> findByEventId(String eventId);

    /**
     * Count events for a signal.
     */
    long countBySignalId(String signalId);

    /**
     * Get last event sequence for a signal.
     */
    @Query("SELECT MAX(e.sequence) FROM ExecutionEventEntity e WHERE e.signalId = :signalId")
    Integer getLastSequenceForSignal(@Param("signalId") String signalId);
}
