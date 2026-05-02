package io.marcus.domain.executor;

/**
 * Port interface for storing and retrieving execution events.
 * Implemented by infrastructure layer (persistence adapters).
 * 
 * Enforces the immutability contract:
 * - Events are write-once: once stored, never modified
 * - Idempotent: storing same eventId twice returns without error
 */
public interface ExecutionEventPort {

    /**
     * Store a new execution event.
     * If eventId already exists for this signalId, return without error (idempotent).
     * 
     * @param event the event to store
     * @return the stored event (or existing event if duplicate)
     * @throws IllegalArgumentException if event validation fails
     */
    ExecutionEvent store(ExecutionEvent event);

    /**
     * Retrieve all events for a signal in sequence order.
     * 
     * @param signalId the signal identifier
     * @return list of events in ascending sequence order
     */
    java.util.List<ExecutionEvent> findBySignalIdOrderBySequence(String signalId);

    /**
     * Retrieve events for a signal within a sequence range.
     * 
     * @param signalId the signal identifier
     * @param fromSequence inclusive lower bound (use 0 for all events)
     * @param toSequence inclusive upper bound (use Integer.MAX_VALUE for all)
     * @return list of events in ascending sequence order
     */
    java.util.List<ExecutionEvent> findBySignalIdAndSequenceRange(
            String signalId,
            int fromSequence,
            int toSequence
    );

    /**
     * Count events for a signal.
     */
    long countBySignalId(String signalId);

    /**
     * Check if an event already exists (by eventId).
     * Used for duplicate detection before storing.
     */
    boolean existsByEventId(String eventId);
}
