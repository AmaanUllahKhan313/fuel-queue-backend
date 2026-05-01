-- Create fuel_stations table
CREATE TABLE IF NOT EXISTS fuel_stations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    geofence_radius_meters DOUBLE,
    active BOOLEAN
);

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
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

