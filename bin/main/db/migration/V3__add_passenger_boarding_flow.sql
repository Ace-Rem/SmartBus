CREATE TABLE passengers (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    phone_number VARCHAR(20) NOT NULL UNIQUE,
    username VARCHAR(60) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE boarding_requests (
    id BIGSERIAL PRIMARY KEY,
    passenger_id BIGINT NOT NULL,
    trip_id BIGINT NOT NULL,
    boarding_stop_id BIGINT NOT NULL,
    destination_stop_id BIGINT NOT NULL,
    passenger_record_id BIGINT,
    status VARCHAR(30) NOT NULL,
    note VARCHAR(255),
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP,
    boarded_at TIMESTAMP,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    CONSTRAINT fk_boarding_requests_passenger FOREIGN KEY (passenger_id) REFERENCES passengers (id),
    CONSTRAINT fk_boarding_requests_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    CONSTRAINT fk_boarding_requests_boarding_stop FOREIGN KEY (boarding_stop_id) REFERENCES stops (id),
    CONSTRAINT fk_boarding_requests_destination_stop FOREIGN KEY (destination_stop_id) REFERENCES stops (id),
    CONSTRAINT fk_boarding_requests_passenger_record FOREIGN KEY (passenger_record_id) REFERENCES passenger_records (id)
);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    passenger_id BIGINT,
    boarding_request_id BIGINT,
    title VARCHAR(120) NOT NULL,
    message VARCHAR(500) NOT NULL,
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_passenger FOREIGN KEY (passenger_id) REFERENCES passengers (id),
    CONSTRAINT fk_notifications_boarding_request FOREIGN KEY (boarding_request_id) REFERENCES boarding_requests (id)
);

CREATE TABLE bluetooth_sessions (
    id BIGSERIAL PRIMARY KEY,
    boarding_request_id BIGINT NOT NULL,
    identifier VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    CONSTRAINT fk_bluetooth_sessions_boarding_request FOREIGN KEY (boarding_request_id) REFERENCES boarding_requests (id)
);

CREATE INDEX idx_boarding_requests_passenger_id ON boarding_requests (passenger_id);
CREATE INDEX idx_boarding_requests_trip_id ON boarding_requests (trip_id);
CREATE INDEX idx_boarding_requests_status ON boarding_requests (status);
CREATE INDEX idx_notifications_passenger_id ON notifications (passenger_id);
CREATE INDEX idx_bluetooth_sessions_identifier ON bluetooth_sessions (identifier);
