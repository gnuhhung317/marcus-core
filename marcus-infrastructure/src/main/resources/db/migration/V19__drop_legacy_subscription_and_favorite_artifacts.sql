ALTER TABLE subscriptions DROP COLUMN IF EXISTS package_id;

DROP TABLE IF EXISTS bot_favorites;
DROP TABLE IF EXISTS bot_asset_pairs;
