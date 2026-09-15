ALTER TABLE ai_client_contexts
    ADD COLUMN IF NOT EXISTS route_id BIGINT;
