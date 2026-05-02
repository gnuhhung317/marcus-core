package io.marcus.domain.executor;

/**
 * Event types for executor-backend synchronization. Defines the complete set of
 * execution events that can occur during trading.
 */
public enum ExecutionEventType {
    // Signal lifecycle
    SIGNAL_ACCEPTED("signal.accepted"),
    SIGNAL_REJECTED("signal.rejected"),
    // Order lifecycle
    ORDER_PLACED("order.placed"),
    ORDER_FAILED("order.failed"),
    ORDER_FILLED("order.filled"),
    ORDER_CANCELED("order.canceled"),
    // Position lifecycle
    POSITION_OPENED("position.opened"),
    POSITION_UPDATED("position.updated"),
    POSITION_CLOSED("position.closed");

    private final String eventTypeCode;

    ExecutionEventType(String eventTypeCode) {
        this.eventTypeCode = eventTypeCode;
    }

    public String getEventTypeCode() {
        return eventTypeCode;
    }

    /**
     * Parse event type code (e.g., "order.placed") to enum.
     */
    public static ExecutionEventType fromCode(String code) {
        for (ExecutionEventType type : ExecutionEventType.values()) {
            if (type.eventTypeCode.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown event type code: " + code);
    }
}
