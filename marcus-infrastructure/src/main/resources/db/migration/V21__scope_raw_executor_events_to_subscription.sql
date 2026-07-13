-- Ownership is needed because several traders can run an executor for the same bot.
-- Historical rows remain unscoped and are intentionally visible only to operator/admin views.
ALTER TABLE raw_events
    ADD COLUMN IF NOT EXISTS user_subscription_id VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_raw_events_subscription_received_at
    ON raw_events(user_subscription_id, received_at DESC);
