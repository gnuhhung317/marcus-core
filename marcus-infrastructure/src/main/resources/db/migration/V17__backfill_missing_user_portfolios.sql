INSERT INTO user_portfolios (
    id,
    user_id,
    total_capital,
    available_balance,
    realized_pnl,
    unrealized_pnl,
    max_drawdown_threshold,
    medium_risk_threshold,
    exchange_id,
    last_sync_at,
    fresh_accounts_count,
    stale_accounts_count,
    data_freshness,
    created_at,
    updated_at
)
SELECT
    md5('user-portfolio-' || u.user_id),
    u.user_id,
    10000,
    10000,
    0,
    0,
    0.1000,
    0.0500,
    NULL,
    NULL,
    0,
    0,
    'STALE',
    NOW(),
    NOW()
FROM users u
LEFT JOIN user_portfolios up ON up.user_id = u.user_id
WHERE up.user_id IS NULL
ON CONFLICT (user_id) DO NOTHING;
