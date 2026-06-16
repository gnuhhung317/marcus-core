ALTER TABLE users
    ADD COLUMN IF NOT EXISTS is_banned BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS banned_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS banned_by_user_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS ban_reason TEXT;

ALTER TABLE subscriptions
    ADD COLUMN IF NOT EXISTS canceled_by_admin_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS cancellation_reason TEXT,
    ADD COLUMN IF NOT EXISTS canceled_at TIMESTAMP WITHOUT TIME ZONE;

CREATE TABLE IF NOT EXISTS admin_audit_events (
    id VARCHAR(36) PRIMARY KEY,
    admin_audit_event_id VARCHAR(255) NOT NULL UNIQUE,
    actor_user_id VARCHAR(255) NOT NULL,
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(100) NOT NULL,
    target_id VARCHAR(255) NOT NULL,
    reason TEXT,
    before_state_json TEXT,
    after_state_json TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_users_role_banned ON users(role, is_banned);
CREATE INDEX IF NOT EXISTS idx_bots_status_developer ON bots(status, developer_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_bot_status ON subscriptions(bot_id, status);
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_status ON subscriptions(user_id, status);
CREATE INDEX IF NOT EXISTS idx_admin_audit_actor_created_at ON admin_audit_events(actor_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_admin_audit_action_created_at ON admin_audit_events(action, created_at DESC);
