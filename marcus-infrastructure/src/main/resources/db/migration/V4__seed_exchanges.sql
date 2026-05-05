INSERT INTO exchanges (id, exchange_id, name, base_url, is_active, created_at, updated_at)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'binance', 'Binance', 'https://api.binance.com', true, NOW(), NOW()),
    ('22222222-2222-2222-2222-222222222222', 'bybit', 'Bybit', 'https://api.bybit.com', true, NOW(), NOW()),
    ('33333333-3333-3333-3333-333333333333', 'okx', 'OKX', 'https://www.okx.com', true, NOW(), NOW())
ON CONFLICT (exchange_id)
DO UPDATE SET
    name = EXCLUDED.name,
    base_url = EXCLUDED.base_url,
    is_active = EXCLUDED.is_active,
    updated_at = NOW(),
    deleted_at = NULL;