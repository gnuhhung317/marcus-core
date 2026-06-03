CREATE TABLE bot_dry_run_portfolios (
    id              VARCHAR(255) PRIMARY KEY,
    bot_id          VARCHAR(255) NOT NULL,
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

CREATE UNIQUE INDEX idx_bot_dry_run_portfolio_bot_time ON bot_dry_run_portfolios (bot_id, timestamp);
CREATE INDEX idx_bot_dry_run_portfolio_time ON bot_dry_run_portfolios (timestamp);

CREATE TABLE bot_dry_run_positions (
    id                VARCHAR(255) PRIMARY KEY,
    bot_id            VARCHAR(255) NOT NULL,
    position_id       VARCHAR(255) NOT NULL,
    symbol            VARCHAR(64) NOT NULL,
    market_type       VARCHAR(32) NOT NULL,
    side              VARCHAR(32) NOT NULL,
    quantity          NUMERIC(18,8) NOT NULL,
    entry_price       NUMERIC(18,8) NOT NULL,
    current_price     NUMERIC(18,8) NOT NULL,
    unrealized_pnl    NUMERIC(18,8) NOT NULL,
    opened_at         TIMESTAMP NOT NULL,
    source_signal_id  VARCHAR(255),
    status            VARCHAR(32) NOT NULL,
    last_synced_at    TIMESTAMP NOT NULL,
    closed_at         TIMESTAMP,
    created_at        TIMESTAMP,
    updated_at        TIMESTAMP,
    deleted_at        TIMESTAMP
);

CREATE UNIQUE INDEX idx_bot_dry_run_position_bot_position ON bot_dry_run_positions (bot_id, position_id);
CREATE INDEX idx_bot_dry_run_position_bot_status ON bot_dry_run_positions (bot_id, status);

CREATE TABLE bot_dry_run_closed_trades (
    id                VARCHAR(255) PRIMARY KEY,
    bot_id            VARCHAR(255) NOT NULL,
    trade_id          VARCHAR(255) NOT NULL,
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
    entry_signal_id   VARCHAR(255),
    exit_signal_id    VARCHAR(255),
    created_at        TIMESTAMP,
    updated_at        TIMESTAMP,
    deleted_at        TIMESTAMP
);

CREATE UNIQUE INDEX idx_bot_dry_run_trade_bot_trade ON bot_dry_run_closed_trades (bot_id, trade_id);
CREATE INDEX idx_bot_dry_run_trade_bot_exit ON bot_dry_run_closed_trades (bot_id, exit_timestamp);
