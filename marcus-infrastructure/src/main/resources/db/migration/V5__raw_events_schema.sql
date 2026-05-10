-- V5__raw_events_schema.sql
-- Append-only raw event log for executor ingest.
-- Persists every incoming message from the external executor for audit and replay.

CREATE TABLE raw_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(255) NOT NULL,
    bot_id VARCHAR(255) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    correlation_id VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,  -- ingest, ack, heartbeat, replay-request, replay-response, control, audit-push
    payload JSONB NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,
    source_conn_id VARCHAR(255) NOT NULL,  -- WebSocket session ID
    sequence_no BIGINT NOT NULL,  -- Monotonic per-bot
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Unique constraint for idempotency: prevent duplicate processing
CREATE UNIQUE INDEX idx_raw_events_bot_idempotency 
ON raw_events(bot_id, idempotency_key);

-- Index for correlation ID queries (debugging and audit)
CREATE INDEX idx_raw_events_correlation_id 
ON raw_events(correlation_id);

-- Index for sequence range queries (replay)
CREATE INDEX idx_raw_events_sequence 
ON raw_events(bot_id, sequence_no);

-- Index for received timestamp (audit range queries)
CREATE INDEX idx_raw_events_received_at 
ON raw_events(received_at);

-- Index for source connection ID (routing replay-response and audit-push)
CREATE INDEX idx_raw_events_source_conn_id 
ON raw_events(source_conn_id);

-- Index for finding unprocessed events (projection worker)
CREATE INDEX idx_raw_events_bot_processed 
ON raw_events(bot_id, processed);

-- Index for event ID lookup (individual event queries)
CREATE INDEX idx_raw_events_event_id 
ON raw_events(event_id);
