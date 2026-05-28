-- Add policies JSONB column to signals table to store execution policies
ALTER TABLE signals
ADD COLUMN IF NOT EXISTS policies jsonb;

-- Optional GIN index to support queries inside policies
CREATE INDEX IF NOT EXISTS idx_signals_policies_gin ON signals USING GIN (policies jsonb_path_ops);
