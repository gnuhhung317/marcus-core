package io.marcus.infrastructure.persistence.executor;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA entity for tracking execution state per signal.
 * Stores: signal state, order state, position state, last sequence number.
 */
@Entity
@Table(
        name = "execution_state",
        indexes = {
                @Index(name = "idx_signal_id_state", columnList = "signal_id", unique = true)
        }
)
public class ExecutionStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "signal_id", nullable = false, unique = true, length = 36)
    private String signalId;

    @Column(name = "signal_state", nullable = false, length = 32)
    private String signalState; // ACCEPTED, REJECTED, OPEN, CLOSED

    @Column(name = "order_state", nullable = false, length = 32)
    private String orderState; // NONE, PLACED, FILLED, FAILED, CANCELED

    @Column(name = "position_state", nullable = false, length = 32)
    private String positionState; // NONE, OPENED, UPDATING, CLOSED

    @Column(name = "last_sequence", nullable = false)
    private Integer lastSequence; // -1 if no events yet

    @Column(name = "last_event_time")
    private Instant lastEventTime;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version; // Optimistic locking

    // Constructors
    public ExecutionStateEntity() {
    }

    public ExecutionStateEntity(
            String signalId,
            String signalState,
            String orderState,
            String positionState,
            Integer lastSequence,
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
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getSignalId() {
        return signalId;
    }

    public String getSignalState() {
        return signalState;
    }

    public String getOrderState() {
        return orderState;
    }

    public String getPositionState() {
        return positionState;
    }

    public Integer getLastSequence() {
        return lastSequence;
    }

    public Instant getLastEventTime() {
        return lastEventTime;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    // Setters
    public void setSignalState(String signalState) {
        this.signalState = signalState;
    }

    public void setOrderState(String orderState) {
        this.orderState = orderState;
    }

    public void setPositionState(String positionState) {
        this.positionState = positionState;
    }

    public void setLastSequence(Integer lastSequence) {
        this.lastSequence = lastSequence;
    }

    public void setLastEventTime(Instant lastEventTime) {
        this.lastEventTime = lastEventTime;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    // JPA callbacks
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    @Override
    public String toString() {
        return "ExecutionStateEntity{" +
                "id=" + id +
                ", signalId='" + signalId + '\'' +
                ", signalState='" + signalState + '\'' +
                ", orderState='" + orderState + '\'' +
                ", positionState='" + positionState + '\'' +
                ", lastSequence=" + lastSequence +
                ", lastEventTime=" + lastEventTime +
                ", closedAt=" + closedAt +
                ", updatedAt=" + updatedAt +
                ", version=" + version +
                '}';
    }
}
