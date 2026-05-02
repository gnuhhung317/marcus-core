package io.marcus.domain.executor;

import java.util.Optional;

/**
 * Port interface for managing execution state per signal. Implemented by
 * infrastructure layer (persistence adapters).
 *
 * Tracks the current lifecycle state of a signal, order, and position.
 */
public interface ExecutionStatePort {

    /**
     * Get the current execution state for a signal.
     *
     * @param signalId the signal identifier
     * @return the current execution state
     */
    Optional<ExecutionState> getState(String signalId);

    /**
     * Create a new execution state for an accepted signal.
     *
     * @param signalId the signal identifier
     * @return the new execution state
     * @throws IllegalArgumentException if signal already has a state
     */
    ExecutionState acceptSignal(String signalId);

    /**
     * Create a new execution state for a rejected signal (terminal).
     *
     * @param signalId the signal identifier
     * @return the new execution state
     * @throws IllegalArgumentException if signal already has a state
     */
    ExecutionState rejectSignal(String signalId);

    /**
     * Update execution state after order is placed.
     *
     * @param signalId the signal identifier
     * @param newLastSequence the new sequence number
     * @param sentAt timestamp of the event
     * @return the updated execution state
     */
    ExecutionState updateOrderPlaced(String signalId, int newLastSequence, java.time.Instant sentAt);

    /**
     * Update execution state after order is filled.
     *
     * @param signalId the signal identifier
     * @param newLastSequence the new sequence number
     * @param sentAt timestamp of the event
     * @return the updated execution state
     */
    ExecutionState updateOrderFilled(String signalId, int newLastSequence, java.time.Instant sentAt);

    /**
     * Update execution state after order fails (terminal for order).
     *
     * @param signalId the signal identifier
     * @param newLastSequence the new sequence number
     * @param sentAt timestamp of the event
     * @return the updated execution state
     */
    ExecutionState updateOrderFailed(String signalId, int newLastSequence, java.time.Instant sentAt);

    /**
     * Update execution state after order is canceled (terminal for order).
     *
     * @param signalId the signal identifier
     * @param newLastSequence the new sequence number
     * @param sentAt timestamp of the event
     * @return the updated execution state
     */
    ExecutionState updateOrderCanceled(String signalId, int newLastSequence, java.time.Instant sentAt);

    /**
     * Update execution state after position opens.
     *
     * @param signalId the signal identifier
     * @param newLastSequence the new sequence number
     * @param sentAt timestamp of the event
     * @return the updated execution state
     */
    ExecutionState updatePositionOpened(String signalId, int newLastSequence, java.time.Instant sentAt);

    /**
     * Update execution state during position updates (PnL changes, etc.).
     *
     * @param signalId the signal identifier
     * @param newLastSequence the new sequence number
     * @param sentAt timestamp of the event
     * @return the updated execution state
     */
    ExecutionState updatePositionUpdated(String signalId, int newLastSequence, java.time.Instant sentAt);

    /**
     * Update execution state after position closes (terminal for signal).
     *
     * @param signalId the signal identifier
     * @param newLastSequence the new sequence number
     * @param sentAt timestamp of the event
     * @return the updated execution state
     */
    ExecutionState updatePositionClosed(String signalId, int newLastSequence, java.time.Instant sentAt);

    /**
     * Check if position is closed (no further events allowed).
     *
     * @param signalId the signal identifier
     * @return true if position is closed
     */
    boolean isPositionClosed(String signalId);

    /**
     * Get the last accepted sequence number for a signal. Returns -1 if no
     * events have been accepted yet.
     *
     * @param signalId the signal identifier
     * @return last accepted sequence or -1
     */
    int getLastSequence(String signalId);
}
