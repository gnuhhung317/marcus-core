CREATE TABLE portfolio_balance_history (
    id              VARCHAR(36) PRIMARY KEY,
    user_id         VARCHAR(255) NOT NULL,
    total           NUMERIC(18,8) NOT NULL,
    free            NUMERIC(18,8) NOT NULL,
    used            NUMERIC(18,8) NOT NULL DEFAULT 0,
    unrealized_pnl  NUMERIC(18,8) NOT NULL DEFAULT 0,
    exchange_id     VARCHAR(255),
    snapshot_at     TIMESTAMP NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ph_user_time ON portfolio_balance_history (user_id, snapshot_at DESC);
