package io.marcus.infrastructure.persistence.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * JPA entity for storing execution events. Immutable once persisted. Payload is
 * stored as JSON text and deserialized on read.
 */
@Entity
@Table(
        name = "execution_event",
        indexes = {
            @Index(name = "idx_signal_id", columnList = "signal_id"),
            @Index(name = "idx_signal_sequence", columnList = "signal_id,sequence"),
            @Index(name = "idx_event_id", columnList = "event_id", unique = true)
        }
)
public class ExecutionEventEntity {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(name = "signal_id", nullable = false, length = 36)
    private String signalId;

    @Column(name = "sequence", nullable = false)
    private Integer sequence;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "exchange_time")
    private Instant exchangeTime;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payloadJson; // Store as JSON string

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Constructors
    public ExecutionEventEntity() {
    }

    public ExecutionEventEntity(
            String eventId,
            String signalId,
            Integer sequence,
            String eventType,
            Instant sentAt,
            Instant exchangeTime,
            Object payload,
            Instant createdAt
    ) {
        this.eventId = eventId;
        this.signalId = signalId;
        this.sequence = sequence;
        this.eventType = eventType;
        this.sentAt = sentAt;
        this.exchangeTime = exchangeTime;
        this.payloadJson = serializePayload(payload);
        this.createdAt = createdAt;
    }

    // Getters (immutable pattern)
    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getSignalId() {
        return signalId;
    }

    public Integer getSequence() {
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

    /**
     * Get payload as JsonNode.
     */
    public JsonNode getPayload() {
        try {
            return OBJECT_MAPPER.readTree(payloadJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize payload: " + e.getMessage(), e);
        }
    }

    /**
     * Get payload as raw JSON string.
     */
    public String getPayloadJson() {
        return payloadJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Serialize payload object to JSON string.
     */
    private static String serializePayload(Object payload) {
        try {
            if (payload == null) {
                return "{}";
            }
            if (payload instanceof String) {
                return (String) payload;
            }
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize payload: " + e.getMessage(), e);
        }
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    @Override
    public String toString() {
        return "ExecutionEventEntity{"
                + "id=" + id
                + ", eventId='" + eventId + '\''
                + ", signalId='" + signalId + '\''
                + ", sequence=" + sequence
                + ", eventType='" + eventType + '\''
                + ", sentAt=" + sentAt
                + ", exchangeTime=" + exchangeTime
                + ", createdAt=" + createdAt
                + '}';
    }
}
