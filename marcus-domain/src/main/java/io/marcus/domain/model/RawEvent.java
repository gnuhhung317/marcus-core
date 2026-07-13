package io.marcus.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.Map;

/**
 * Append-only raw event from the external executor. Represents a single message
 * in the ingest stream.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RawEvent extends BaseModel {

    private String id;
    private String eventId;
    private String botId;
    /** Owning subscription when this event originates from a local executor. */
    private String userSubscriptionId;
    private String idempotencyKey;
    private String correlationId;
    private String type;  // ingest, ack, heartbeat, replay-request, etc.
    private Map<String, Object> payload;
    private Instant receivedAt;
    private String sourceConnId;
    private Long sequenceNo;
    private Boolean processed;
    private Instant processedAt;
}
