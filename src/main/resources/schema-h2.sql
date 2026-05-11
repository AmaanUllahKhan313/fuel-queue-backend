-- Create fuel_stations table
CREATE TABLE IF NOT EXISTS fuel_stations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    geofence_radius_meters DOUBLE,
    active BOOLEAN,
    is_live BOOLEAN DEFAULT FALSE
);

ALTER TABLE fuel_stations ADD COLUMN IF NOT EXISTS is_live BOOLEAN DEFAULT FALSE;
UPDATE fuel_stations SET is_live = FALSE WHERE is_live IS NULL;

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone_number VARCHAR(20) NOT NULL UNIQUE,
    phone_verified BOOLEAN DEFAULT FALSE,
    otp VARCHAR(6),
    otp_expires_at TIMESTAMP,
    name VARCHAR(255),
    fcm_token VARCHAR(255)
);

-- Create location_pings table
CREATE TABLE IF NOT EXISTS location_pings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    fuel_station_id BIGINT,
    latitude DOUBLE,
    longitude DOUBLE,
    created_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (fuel_station_id) REFERENCES fuel_stations(id)
);

ALTER TABLE fuel_stations
ADD CONSTRAINT unique_station UNIQUE (name, latitude, longitude);
