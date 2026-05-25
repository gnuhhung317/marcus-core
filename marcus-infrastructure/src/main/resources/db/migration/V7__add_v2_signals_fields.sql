-- Alter signals table to add missing v2 fields safely
ALTER TABLE signals ADD COLUMN IF NOT EXISTS market_type VARCHAR(50);
ALTER TABLE signals ADD COLUMN IF NOT EXISTS order_type VARCHAR(50);

-- Provide default values for existing records (if any) to satisfy NOT NULL constraints
UPDATE signals SET market_type = 'SPOT' WHERE market_type IS NULL;
UPDATE signals SET order_type = 'MARKET' WHERE order_type IS NULL;

-- Set NOT NULL constraints for required fields
ALTER TABLE signals ALTER COLUMN market_type SET NOT NULL;
ALTER TABLE signals ALTER COLUMN order_type SET NOT NULL;
