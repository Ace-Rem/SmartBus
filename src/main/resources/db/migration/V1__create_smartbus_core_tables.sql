CREATE TABLE drivers (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    phone_number VARCHAR(20) NOT NULL UNIQUE,
    license_number VARCHAR(50) NOT NULL UNIQUE,
    username VARCHAR(60) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE routes (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE stops (
    id BIGSERIAL PRIMARY KEY,
    route_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    latitude NUMERIC(10, 7) NOT NULL,
    longitude NUMERIC(10, 7) NOT NULL,
    stop_order INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_stops_route FOREIGN KEY (route_id) REFERENCES routes (id),
    CONSTRAINT uq_stops_route_order UNIQUE (route_id, stop_order)
);

CREATE TABLE trips (
    id BIGSERIAL PRIMARY KEY,
    driver_id BIGINT NOT NULL,
    route_id BIGINT NOT NULL,
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    status VARCHAR(30) NOT NULL,
    current_latitude NUMERIC(10, 7),
    current_longitude NUMERIC(10, 7),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_trips_driver FOREIGN KEY (driver_id) REFERENCES drivers (id),
    CONSTRAINT fk_trips_route FOREIGN KEY (route_id) REFERENCES routes (id)
);

CREATE TABLE passenger_records (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    stop_id BIGINT,
    passenger_count INTEGER NOT NULL,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note VARCHAR(255),
    CONSTRAINT fk_passenger_records_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    CONSTRAINT fk_passenger_records_stop FOREIGN KEY (stop_id) REFERENCES stops (id)
);

CREATE INDEX idx_stops_route_id ON stops (route_id);
CREATE INDEX idx_trips_driver_id ON trips (driver_id);
CREATE INDEX idx_trips_route_id ON trips (route_id);
CREATE INDEX idx_passenger_records_trip_id ON passenger_records (trip_id);
