CREATE TABLE bot_telemetry_points (
    id              VARCHAR(255) PRIMARY KEY,
    bot_id          VARCHAR(255) NOT NULL,
    timestamp       TIMESTAMP NOT NULL,
    equity          NUMERIC(18,8) NOT NULL,
    realized_pnl    NUMERIC(18,8) NOT NULL,
    unrealized_pnl  NUMERIC(18,8) NOT NULL,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP,
    deleted_at      TIMESTAMP
);

CREATE UNIQUE INDEX idx_bot_telemetry_bot_time ON bot_telemetry_points (bot_id, timestamp);
CREATE INDEX idx_bot_telemetry_time ON bot_telemetry_points (timestamp);
