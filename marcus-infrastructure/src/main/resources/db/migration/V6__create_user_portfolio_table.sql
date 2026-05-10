-- Creates the user portfolio repository for tracking capital assets
CREATE TABLE user_portfolios (
    id                      VARCHAR(36) PRIMARY KEY,
    user_id                 VARCHAR(255) NOT NULL,
    total_capital           NUMERIC(18,8) NOT NULL DEFAULT 10000,
    available_balance       NUMERIC(18,8),
    realized_pnl            NUMERIC(18,8),
    unrealized_pnl          NUMERIC(18,8),
    max_drawdown_threshold   NUMERIC(5,4) NOT NULL DEFAULT 0.1000,
    medium_risk_threshold    NUMERIC(5,4) NOT NULL DEFAULT 0.0500,
    exchange_id             VARCHAR(255),
    last_sync_at            TIMESTAMP,
    created_at              TIMESTAMP NOT NULL,
    updated_at              TIMESTAMP NOT NULL,
    deleted_at              TIMESTAMP,
    CONSTRAINT uk_user_portfolios_user_id UNIQUE (user_id)
);

COMMENT ON TABLE user_portfolios IS 'Tracks user dynamic capital, live balance sync from python executor, and configurable custom risk boundaries';
