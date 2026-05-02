package io.marcus.domain.executor;

import java.time.Instant;

/**
 * Represents the execution state of a signal in the executor-backend sync
 * protocol. Tracks: signal state, order state, position state, and sequence
 * number.
 */
public class ExecutionState {

    private final String signalId;
    private final SignalState signalState;
    private final OrderState orderState;
    private final PositionState positionState;
    private final int lastSequence;
    private final Instant lastEventTime;
    private final Instant closedAt;

    public ExecutionState(
            String signalId,
            SignalState signalState,
            OrderState orderState,
            PositionState positionState,
            int lastSequence,
            Instant lastEventTime,
            Instant closedAt
    ) {
        this.signalId = signalId;
        this.signalState = signalState;
        this.orderState = orderState;
        this.positionState = positionState;
        this.lastSequence = lastSequence;
        this.lastEventTime = lastEventTime;
        this.closedAt = closedAt;
    }

    /**
     * Create a new execution state for an accepted signal.
     */
    public static ExecutionState accepted(String signalId) {
        return new ExecutionState(
                signalId,
                SignalState.ACCEPTED,
                OrderState.NONE,
                PositionState.NONE,
                -1,
                null,
                null
        );
    }

    /**
     * Create a new execution state for a rejected signal (terminal).
     */
    public static ExecutionState rejected(String signalId, Instant rejectedAt) {
        return new ExecutionState(
                signalId,
                SignalState.REJECTED,
                OrderState.NONE,
                PositionState.NONE,
                0,
                rejectedAt,
                rejectedAt
        );
    }

    // Getters
    public String getSignalId() {
        return signalId;
    }

    public SignalState getSignalState() {
        return signalState;
    }

    public OrderState getOrderState() {
        return orderState;
    }

    public PositionState getPositionState() {
        return positionState;
    }

    public int getLastSequence() {
        return lastSequence;
    }

    public Instant getLastEventTime() {
        return lastEventTime;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    /**
     * Check if this state allows further events. Returns false if signal or
     * position is closed (terminal).
     */
    public boolean allowsFurtherEvents() {
        return signalState != SignalState.REJECTED
                && signalState != SignalState.CLOSED
                && positionState != PositionState.CLOSED;
    }

    /**
     * Check if position is closed (terminal state).
     */
    public boolean isPositionClosed() {
        return positionState == PositionState.CLOSED;
    }

    /**
     * Signal lifecycle states.
     */
    public enum SignalState {
        ACCEPTED, // Signal received and validated
        REJECTED, // Signal rejected (terminal)
        OPEN, // Orders placed or filled
        CLOSED       // Position closed (terminal)
    }

    /**
     * Order lifecycle states.
     */
    public enum OrderState {
        NONE, // No order yet
        PLACED, // Order submitted to exchange
        FILLED, // Order executed (may be partial)
        FAILED, // Order rejected (terminal)
        CANCELED     // Order canceled (terminal)
    }

    /**
     * Position lifecycle states.
     */
    public enum PositionState {
        NONE, // No position yet
        OPENED, // Position created from filled order
        UPDATING, // Position open, receiving updates
        CLOSED       // Position closed (terminal)
    }

    @Override
    public String toString() {
        return "ExecutionState{"
                + "signalId='" + signalId + '\''
                + ", signalState=" + signalState
                + ", orderState=" + orderState
                + ", positionState=" + positionState
                + ", lastSequence=" + lastSequence
                + ", lastEventTime=" + lastEventTime
                + ", closedAt=" + closedAt
                + '}';
    }
}
