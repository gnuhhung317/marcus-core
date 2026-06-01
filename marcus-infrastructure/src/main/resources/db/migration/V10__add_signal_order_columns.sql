-- Add order tracking columns to signals for executor wiring
ALTER TABLE signals
ADD COLUMN IF NOT EXISTS order_id TEXT;

ALTER TABLE signals
ADD COLUMN IF NOT EXISTS order_symbol TEXT;

-- Optional index to help lookups by order_id
CREATE INDEX IF NOT EXISTS idx_signals_order_id ON signals(order_id);