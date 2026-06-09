CREATE TABLE bot_favorites (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    bot_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT uk_bot_favorites_user_bot UNIQUE (user_id, bot_id)
);

CREATE INDEX idx_bot_favorites_bot_id ON bot_favorites (bot_id);
