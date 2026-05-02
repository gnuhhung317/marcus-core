package io.marcus.domain.executor;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable value object representing an execution event from the executor
 * client. Once created, an ExecutionEvent cannot be modified. Payload is stored
 * as a generic Object to keep domain layer framework-independent.
 */
public final class ExecutionEvent {

    private final String eventId;
    private final String signalId;
    private final int sequence;
    private final ExecutionEventType eventType;
    private final Instant sentAt;
    private final Instant exchangeTime;
    private final Object payload; // Generic payload (will be JsonNode in infrastructure)
    private final Instant createdAt;

    public ExecutionEvent(
            String eventId,
            String signalId,
            int sequence,
            ExecutionEventType eventType,
            Instant sentAt,
            Instant exchangeTime,
            Object payload,
            Instant createdAt
    ) {
        this.eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        this.signalId = Objects.requireNonNull(signalId, "signalId must not be null");
        if (sequence < 0) {
            throw new IllegalArgumentException("sequence must be >= 0");
        }
        this.sequence = sequence;
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.sentAt = Objects.requireNonNull(sentAt, "sentAt must not be null");
        this.exchangeTime = exchangeTime; // nullable
        this.payload = Objects.requireNonNull(payload, "payload must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    /**
     * Create a new ExecutionEvent with current timestamp.
     */
    public static ExecutionEvent create(
            String eventId,
            String signalId,
            int sequence,
            ExecutionEventType eventType,
            Instant sentAt,
            Instant exchangeTime,
            Object payload
    ) {
        return new ExecutionEvent(eventId, signalId, sequence, eventType, sentAt, exchangeTime, payload, Instant.now());
    }

    // Immutable getters
    public String getEventId() {
        return eventId;
    }

    public String getSignalId() {
        return signalId;
    }

    public int getSequence() {
        return sequence;
    }

    public ExecutionEventType getEventType() {
        return eventType;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getExchangeTime() {
        return exchangeTime;
    }

    public Object getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Check if this event is a position-closing event (terminal for signal).
     */
    public boolean isPositionClosing() {
        return eventType == ExecutionEventType.POSITION_CLOSED
                || eventType == ExecutionEventType.SIGNAL_REJECTED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExecutionEvent that = (ExecutionEvent) o;
        return sequence == that.sequence
                && Objects.equals(eventId, that.eventId)
                && Objects.equals(signalId, that.signalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, signalId, sequence);
    }

    @Override
    public String toString() {
        return "ExecutionEvent{"
                + "eventId='" + eventId + '\''
                + ", signalId='" + signalId + '\''
                + ", sequence=" + sequence
                + ", eventType=" + eventType
                + ", sentAt=" + sentAt
                + ", exchangeTime=" + exchangeTime
                + ", createdAt=" + createdAt
                + '}';
    }
}
