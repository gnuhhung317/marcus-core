CREATE TABLE subscriptions (
    id VARCHAR(36) PRIMARY KEY,
    user_subscription_id VARCHAR(255) NOT NULL UNIQUE,
    user_id VARCHAR(255) NOT NULL,
    bot_id VARCHAR(255) NOT NULL,
    package_id VARCHAR(255),
    ws_token VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    start_date TIMESTAMP WITHOUT TIME ZONE,
    end_date TIMESTAMP WITHOUT TIME ZONE,
    executor_connected BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT uk_subscriptions_user_bot_status UNIQUE (user_id, bot_id, status)
);

CREATE INDEX idx_subscriptions_user_status ON subscriptions(user_id, status);
CREATE INDEX idx_subscriptions_bot_status ON subscriptions(bot_id, status);
