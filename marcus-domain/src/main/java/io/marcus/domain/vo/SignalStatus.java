package io.marcus.domain.vo;

public enum SignalStatus {
    RECEIVED,
    VALIDATED,
    BROADCASTED,
    DISPATCHED,
    ACKNOWLEDGED,
    /** Signal arrived at executor after its {@code cancel_after_timestamp} deadline. */
    EXPIRED,
    FAILED_DELIVERY,
    FAILED
}
