package io.marcus.application.executor;

import java.time.Instant;
import java.util.Objects;

/**
 * Output DTO for SyncExecutionEventUseCase. Represents the ACK response sent
 * back to executor client.
 */
public class SyncExecutionEventOutput {

    private final String eventId;
    private final String signalId;
    private final String status; // "OK" or "ERROR"
    private final String errorCode; // e.g., "OUT_OF_ORDER", "POSITION_CLOSED", null if OK
    private final String errorMessage;
    private final Instant receivedAt;

    public SyncExecutionEventOutput(
            String eventId,
            String signalId,
            String status,
            String errorCode,
            String errorMessage,
            Instant receivedAt
    ) {
        this.eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        this.signalId = Objects.requireNonNull(signalId, "signalId must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.errorCode = errorCode; // nullable
        this.errorMessage = errorMessage; // nullable
        this.receivedAt = Objects.requireNonNull(receivedAt, "receivedAt must not be null");
    }

    /**
     * Create a successful ACK response.
     */
    public static SyncExecutionEventOutput ok(String eventId, String signalId, Instant receivedAt) {
        return new SyncExecutionEventOutput(eventId, signalId, "OK", null, null, receivedAt);
    }

    /**
     * Create an error ACK response.
     */
    public static SyncExecutionEventOutput error(
            String eventId,
            String signalId,
            String errorCode,
            String errorMessage,
            Instant receivedAt
    ) {
        return new SyncExecutionEventOutput(eventId, signalId, "ERROR", errorCode, errorMessage, receivedAt);
    }

    public String getEventId() {
        return eventId;
    }

    public String getSignalId() {
        return signalId;
    }

    public String getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public boolean isSuccess() {
        return "OK".equals(status);
    }
}
