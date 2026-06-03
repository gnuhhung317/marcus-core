ALTER TABLE bot_dry_run_portfolios
    ADD COLUMN IF NOT EXISTS data_source VARCHAR(32) NOT NULL DEFAULT 'OUT_OF_SAMPLE';

ALTER TABLE bot_dry_run_positions
    ADD COLUMN IF NOT EXISTS data_source VARCHAR(32) NOT NULL DEFAULT 'OUT_OF_SAMPLE';

ALTER TABLE bot_dry_run_closed_trades
    ADD COLUMN IF NOT EXISTS data_source VARCHAR(32) NOT NULL DEFAULT 'OUT_OF_SAMPLE';

ALTER TABLE bot_telemetry_points
    ADD COLUMN IF NOT EXISTS metrics_json TEXT;

CREATE TABLE bot_backtest_runs (
    run_id       VARCHAR(255) PRIMARY KEY,
    bot_id       VARCHAR(255) NOT NULL,
    run_name     VARCHAR(255),
    started_at   TIMESTAMP,
    ended_at     TIMESTAMP,
    metrics_json TEXT,
    created_at   TIMESTAMP,
    updated_at   TIMESTAMP,
    deleted_at   TIMESTAMP
);

CREATE INDEX idx_bot_backtest_run_bot_created ON bot_backtest_runs (bot_id, created_at);

CREATE TABLE bot_historical_portfolios (
    id              VARCHAR(255) PRIMARY KEY,
    run_id          VARCHAR(255) NOT NULL,
    bot_id          VARCHAR(255) NOT NULL,
    data_source     VARCHAR(32) NOT NULL DEFAULT 'HISTORICAL',
    timestamp       TIMESTAMP NOT NULL,
    cash            NUMERIC(18,8) NOT NULL,
    equity          NUMERIC(18,8) NOT NULL,
    realized_pnl    NUMERIC(18,8) NOT NULL,
    unrealized_pnl  NUMERIC(18,8) NOT NULL,
    total_fees      NUMERIC(18,8) NOT NULL,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP,
    deleted_at      TIMESTAMP
);

CREATE UNIQUE INDEX idx_bot_historical_portfolio_run_time ON bot_historical_portfolios (run_id, timestamp);
CREATE INDEX idx_bot_historical_portfolio_bot_time ON bot_historical_portfolios (bot_id, timestamp);

CREATE TABLE bot_historical_closed_trades (
    id                VARCHAR(255) PRIMARY KEY,
    run_id            VARCHAR(255) NOT NULL,
    bot_id            VARCHAR(255) NOT NULL,
    trade_id          VARCHAR(255) NOT NULL,
    data_source       VARCHAR(32) NOT NULL DEFAULT 'HISTORICAL',
    symbol            VARCHAR(64) NOT NULL,
    market_type       VARCHAR(32) NOT NULL,
    side              VARCHAR(32) NOT NULL,
    quantity          NUMERIC(18,8) NOT NULL,
    entry_price       NUMERIC(18,8) NOT NULL,
    exit_price        NUMERIC(18,8) NOT NULL,
    pnl               NUMERIC(18,8) NOT NULL,
    fees              NUMERIC(18,8) NOT NULL,
    entry_timestamp   TIMESTAMP NOT NULL,
    exit_timestamp    TIMESTAMP NOT NULL,
    duration_seconds  NUMERIC(18,8) NOT NULL,
    created_at        TIMESTAMP,
    updated_at        TIMESTAMP,
    deleted_at        TIMESTAMP
);

CREATE UNIQUE INDEX idx_bot_historical_trade_run_trade ON bot_historical_closed_trades (run_id, trade_id);
CREATE INDEX idx_bot_historical_trade_bot_exit ON bot_historical_closed_trades (bot_id, exit_timestamp);
