package io.marcus.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * Append-only raw event log for executor ingest. Persists every incoming
 * message from the external executor for audit and replay.
 */
@Entity
@Table(
        name = "raw_events",
        indexes = {
            @Index(name = "idx_raw_events_bot_id", columnList = "bot_id"),
            @Index(name = "idx_raw_events_bot_idempotency", columnList = "bot_id,idempotency_key", unique = true),
            @Index(name = "idx_raw_events_correlation_id", columnList = "correlation_id"),
            @Index(name = "idx_raw_events_sequence", columnList = "bot_id,sequence_no"),
            @Index(name = "idx_raw_events_received_at", columnList = "received_at"),
            @Index(name = "idx_raw_events_source_conn_id", columnList = "source_conn_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RawEventEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * Unique event identifier from the executor (eventId in envelope).
     */
    @Column(nullable = false)
    private String eventId;

    /**
     * Bot identifier - used for multi-tenancy and sequence ordering.
     */
    @Column(nullable = false)
    private String botId;

    /**
     * Idempotency key from the executor message envelope. Unique constraint
     * with botId prevents duplicate processing.
     */
    @Column(nullable = false)
    private String idempotencyKey;

    /**
     * Correlation ID for tracing cross-system requests. Mandatory for
     * decision-impact messages.
     */
    @Column(nullable = false)
    private String correlationId;

    /**
     * Message type: ingest, ack, heartbeat, replay-request, replay-response,
     * control, audit-push.
     */
    @Column(nullable = false)
    private String type;

    /**
     * Raw JSON payload from the executor envelope.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> payload;

    /**
     * Server timestamp when the message was received (Instant for precision).
     */
    @Column(nullable = false)
    private Instant receivedAt;

    /**
     * Connection identifier (e.g., WebSocket session ID) that sent this event.
     * Used to route replay-response and audit-push messages back to the correct
     * executor.
     */
    @Column(nullable = false)
    private String sourceConnId;

    /**
     * Monotonic sequence number per bot. Used for ordering events when
     * out-of-order delivery occurs. Assigned by the server on persist.
     */
    @Column(nullable = false)
    private Long sequenceNo;

    /**
     * Whether this event has been processed by the projection worker.
     */
    @Column(nullable = false)
    private Boolean processed;

    /**
     * Timestamp when projection processing completed (null if not yet
     * processed).
     */
    private Instant processedAt;
}
