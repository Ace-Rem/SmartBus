CREATE TABLE IF NOT EXISTS ai_client_contexts (
    id BIGSERIAL PRIMARY KEY,
    owner_type VARCHAR(20) NOT NULL,
    owner_id BIGINT NOT NULL,
    client_key VARCHAR(160) NOT NULL,
    trip_id BIGINT,
    context_json TEXT NOT NULL,
    client_version BIGINT,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_ai_client_context_owner_trip
        UNIQUE (owner_type, owner_id, client_key)
);

CREATE INDEX IF NOT EXISTS idx_ai_client_context_trip
    ON ai_client_contexts (trip_id);
