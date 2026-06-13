ALTER TABLE user_portfolios
    ADD COLUMN IF NOT EXISTS fresh_accounts_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS stale_accounts_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS data_freshness VARCHAR(16) NOT NULL DEFAULT 'STALE';

CREATE TABLE IF NOT EXISTS portfolio_accounts (
    id                  VARCHAR(36) PRIMARY KEY,
    user_id             VARCHAR(255) NOT NULL,
    user_subscription_id VARCHAR(255) NOT NULL,
    bot_id              VARCHAR(255) NOT NULL,
    ws_token            VARCHAR(255) NOT NULL,
    exchange_id         VARCHAR(255),
    currency            VARCHAR(32),
    execution_mode      VARCHAR(32),
    total               NUMERIC(18,8) NOT NULL DEFAULT 0,
    free                NUMERIC(18,8) NOT NULL DEFAULT 0,
    used                NUMERIC(18,8) NOT NULL DEFAULT 0,
    realized_pnl        NUMERIC(18,8) NOT NULL DEFAULT 0,
    unrealized_pnl      NUMERIC(18,8) NOT NULL DEFAULT 0,
    last_sync_at        TIMESTAMP,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMP,
    CONSTRAINT uk_portfolio_accounts_subscription UNIQUE (user_subscription_id)
);

CREATE INDEX IF NOT EXISTS idx_portfolio_accounts_user_sync
    ON portfolio_accounts (user_id, last_sync_at DESC);

ALTER TABLE portfolio_balance_history
    ADD COLUMN IF NOT EXISTS user_subscription_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS bot_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS currency VARCHAR(32),
    ADD COLUMN IF NOT EXISTS execution_mode VARCHAR(32),
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE IF NOT EXISTS portfolio_aggregate_history (
    id                  VARCHAR(36) PRIMARY KEY,
    user_id             VARCHAR(255) NOT NULL,
    total               NUMERIC(18,8) NOT NULL DEFAULT 0,
    free                NUMERIC(18,8) NOT NULL DEFAULT 0,
    used                NUMERIC(18,8) NOT NULL DEFAULT 0,
    realized_pnl        NUMERIC(18,8) NOT NULL DEFAULT 0,
    unrealized_pnl      NUMERIC(18,8) NOT NULL DEFAULT 0,
    fresh_accounts_count INTEGER NOT NULL DEFAULT 0,
    stale_accounts_count INTEGER NOT NULL DEFAULT 0,
    data_freshness      VARCHAR(16) NOT NULL DEFAULT 'STALE',
    exchange_id         VARCHAR(255),
    snapshot_at         TIMESTAMP NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pah_user_time
    ON portfolio_aggregate_history (user_id, snapshot_at DESC);

INSERT INTO portfolio_accounts (
    id,
    user_id,
    user_subscription_id,
    bot_id,
    ws_token,
    exchange_id,
    currency,
    execution_mode,
    total,
    free,
    used,
    realized_pnl,
    unrealized_pnl,
    last_sync_at,
    is_active,
    created_at,
    updated_at
)
SELECT
    md5('legacy-account-' || up.user_id),
    up.user_id,
    'legacy-' || up.user_id,
    COALESCE(up.exchange_id, 'legacy'),
    COALESCE(up.exchange_id, 'legacy'),
    up.exchange_id,
    'USDT',
    'legacy',
    COALESCE(up.total_capital, 0),
    COALESCE(up.available_balance, 0),
    GREATEST(COALESCE(up.total_capital, 0) - COALESCE(up.available_balance, 0), 0),
    COALESCE(up.realized_pnl, 0),
    COALESCE(up.unrealized_pnl, 0),
    up.last_sync_at,
    TRUE,
    NOW(),
    NOW()
FROM user_portfolios up
ON CONFLICT (user_subscription_id) DO NOTHING;

INSERT INTO portfolio_aggregate_history (
    id,
    user_id,
    total,
    free,
    used,
    realized_pnl,
    unrealized_pnl,
    fresh_accounts_count,
    stale_accounts_count,
    data_freshness,
    exchange_id,
    snapshot_at,
    created_at,
    updated_at
)
SELECT
    md5('legacy-aggregate-' || up.user_id),
    up.user_id,
    COALESCE(up.total_capital, 0),
    COALESCE(up.available_balance, 0),
    GREATEST(COALESCE(up.total_capital, 0) - COALESCE(up.available_balance, 0), 0),
    COALESCE(up.realized_pnl, 0),
    COALESCE(up.unrealized_pnl, 0),
    CASE WHEN up.last_sync_at IS NULL THEN 0 ELSE 1 END,
    CASE WHEN up.last_sync_at IS NULL THEN 1 ELSE 0 END,
    CASE WHEN up.last_sync_at IS NULL THEN 'STALE' ELSE 'FRESH' END,
    up.exchange_id,
    COALESCE(up.last_sync_at, NOW()),
    NOW(),
    NOW()
FROM user_portfolios up
ON CONFLICT (id) DO NOTHING;
