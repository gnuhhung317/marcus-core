package io.marcus.application.executor;

import java.time.Instant;
import java.util.Objects;

/**
 * Input DTO for SyncExecutionEventUseCase. Represents an incoming execution
 * event from the executor client.
 */
public class SyncExecutionEventInput {

    private final String eventId;
    private final String signalId;
    private final int sequence;
    private final String eventType; // e.g., "order.placed", "position.closed"
    private final Instant sentAt;
    private final Instant exchangeTime;
    private final Object payload; // Generic payload (typically JsonNode or Map)

    public SyncExecutionEventInput(
            String eventId,
            String signalId,
            int sequence,
            String eventType,
            Instant sentAt,
            Instant exchangeTime,
            Object payload
    ) {
        this.eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        this.signalId = Objects.requireNonNull(signalId, "signalId must not be null");
        this.sequence = sequence;
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.sentAt = Objects.requireNonNull(sentAt, "sentAt must not be null");
        this.exchangeTime = exchangeTime; // nullable
        this.payload = Objects.requireNonNull(payload, "payload must not be null");
    }

    public String getEventId() {
        return eventId;
    }

    public String getSignalId() {
        return signalId;
    }

    public int getSequence() {
        return sequence;
    }

    public String getEventType() {
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
}
