ALTER TABLE passenger_records
    ADD COLUMN IF NOT EXISTS boarding_stop_id BIGINT,
    ADD COLUMN IF NOT EXISTS source VARCHAR(30),
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(160);

ALTER TABLE passenger_records
    ADD CONSTRAINT fk_passenger_records_boarding_stop
    FOREIGN KEY (boarding_stop_id) REFERENCES stops (id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_passenger_records_trip_event
    ON passenger_records (trip_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_passenger_records_source
    ON passenger_records (source);
