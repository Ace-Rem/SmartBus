ALTER TABLE trips
    ADD COLUMN current_stop_id BIGINT;

ALTER TABLE trips
    ADD CONSTRAINT fk_trips_current_stop
        FOREIGN KEY (current_stop_id) REFERENCES stops (id);

CREATE INDEX idx_trips_current_stop_id ON trips (current_stop_id);
