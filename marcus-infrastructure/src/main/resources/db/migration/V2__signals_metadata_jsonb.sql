-- Convert signals.metadata from TEXT to JSONB while preserving legacy plain-text payloads.
CREATE OR REPLACE FUNCTION try_parse_jsonb(input_text TEXT)
RETURNS JSONB
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN input_text::jsonb;
EXCEPTION
    WHEN OTHERS THEN
        RETURN jsonb_build_object('raw', input_text);
END;
$$;

ALTER TABLE signals
    ALTER COLUMN metadata TYPE JSONB
    USING CASE
        WHEN metadata IS NULL OR btrim(metadata) = '' THEN NULL
        ELSE try_parse_jsonb(metadata)
    END;

DROP FUNCTION try_parse_jsonb(TEXT);

CREATE INDEX IF NOT EXISTS idx_signals_metadata_gin ON signals USING GIN (metadata);
