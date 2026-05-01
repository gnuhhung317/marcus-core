-- Migration V3: Create execution_event and execution_state tables for executor event sync

-- execution_event: Immutable log of all events from executor
CREATE TABLE execution_event (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL UNIQUE,
    signal_id VARCHAR(36) NOT NULL,
    sequence INTEGER NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE NOT NULL,
    exchange_time TIMESTAMP WITH TIME ZONE,
    payload TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_execution_event_event_id UNIQUE (event_id)
);

CREATE INDEX idx_execution_event_signal_id ON execution_event(signal_id);
CREATE INDEX idx_execution_event_signal_sequence ON execution_event(signal_id, sequence);
CREATE INDEX idx_execution_event_event_id ON execution_event(event_id);

-- execution_state: Current state of signal/order/position per signal
CREATE TABLE execution_state (
    id BIGSERIAL PRIMARY KEY,
    signal_id VARCHAR(36) NOT NULL UNIQUE,
    signal_state VARCHAR(32) NOT NULL,
    order_state VARCHAR(32) NOT NULL,
    position_state VARCHAR(32) NOT NULL,
    last_sequence INTEGER NOT NULL DEFAULT 0,
    last_event_time TIMESTAMP WITH TIME ZONE,
    closed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_execution_state_signal_id UNIQUE (signal_id)
);

CREATE INDEX idx_execution_state_signal_id ON execution_state(signal_id);
CREATE INDEX idx_execution_state_signal_state ON execution_state(signal_id, signal_state);
