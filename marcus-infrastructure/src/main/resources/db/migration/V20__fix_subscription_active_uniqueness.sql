ALTER TABLE subscriptions
    DROP CONSTRAINT IF EXISTS uk_subscriptions_user_bot_status;

CREATE UNIQUE INDEX IF NOT EXISTS uk_subscriptions_user_bot_active
    ON subscriptions (user_id, bot_id)
    WHERE status = 'ACTIVE';
