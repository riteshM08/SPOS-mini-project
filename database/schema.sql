
DROP TABLE IF EXISTS payments CASCADE;
DROP TABLE IF EXISTS passengers CASCADE;
DROP TABLE IF EXISTS bookings CASCADE;
DROP TABLE IF EXISTS seats CASCADE;
DROP TABLE IF EXISTS flights CASCADE;
DROP TABLE IF EXISTS airports CASCADE;
DROP TABLE IF EXISTS users CASCADE;

CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(120) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    salt VARCHAR(64) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'passenger' CHECK (role IN ('passenger', 'admin')),
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

CREATE TABLE airports (
    airport_id SERIAL PRIMARY KEY,
    airport_code VARCHAR(3) UNIQUE NOT NULL,
    airport_name VARCHAR(150) NOT NULL,
    city VARCHAR(80) NOT NULL,
    country VARCHAR(80) NOT NULL
);

CREATE INDEX idx_airports_code ON airports(airport_code);
CREATE INDEX idx_airports_city ON airports(city);

CREATE TABLE flights (
    flight_id SERIAL PRIMARY KEY,
    flight_number VARCHAR(20) UNIQUE NOT NULL,
    airline_name VARCHAR(80) NOT NULL,
    source_airport_id INT NOT NULL REFERENCES airports(airport_id) ON DELETE RESTRICT,
    destination_airport_id INT NOT NULL REFERENCES airports(airport_id) ON DELETE RESTRICT,
    departure_time TIMESTAMP NOT NULL,
    arrival_time TIMESTAMP NOT NULL,
    duration_minutes INT NOT NULL,
    fare DECIMAL(10, 2) NOT NULL CHECK (fare > 0),
    total_seats INT NOT NULL DEFAULT 60 CHECK (total_seats > 0),
    available_seats INT NOT NULL DEFAULT 60 CHECK (available_seats >= 0),
    aircraft_model VARCHAR(60) DEFAULT 'Boeing 737-800',
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED', 'ACTIVE', 'DELAYED', 'COMPLETED', 'CANCELLED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_different_airports CHECK (source_airport_id <> destination_airport_id),
    CONSTRAINT chk_valid_times CHECK (arrival_time > departure_time)
);

CREATE INDEX idx_flights_search ON flights(source_airport_id, destination_airport_id, departure_time, status);
CREATE INDEX idx_flights_number ON flights(flight_number);

CREATE TABLE seats (
    seat_id SERIAL PRIMARY KEY,
    flight_id INT NOT NULL REFERENCES flights(flight_id) ON DELETE CASCADE,
    seat_number VARCHAR(10) NOT NULL,
    seat_class VARCHAR(20) NOT NULL DEFAULT 'ECONOMY' CHECK (seat_class IN ('ECONOMY', 'BUSINESS', 'FIRST')),
    is_booked BOOLEAN NOT NULL DEFAULT FALSE,
    price_multiplier DECIMAL(3, 2) NOT NULL DEFAULT 1.00,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_flight_seat UNIQUE (flight_id, seat_number)
);

CREATE INDEX idx_seats_flight_status ON seats(flight_id, is_booked);

CREATE TABLE bookings (
    booking_id SERIAL PRIMARY KEY,
    pnr VARCHAR(10) UNIQUE NOT NULL,
    user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    flight_id INT NOT NULL REFERENCES flights(flight_id) ON DELETE RESTRICT,
    booking_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    passenger_count INT NOT NULL DEFAULT 1 CHECK (passenger_count > 0),
    total_fare DECIMAL(10, 2) NOT NULL CHECK (total_fare >= 0),
    booking_status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED' CHECK (booking_status IN ('CONFIRMED', 'CANCELLED', 'COMPLETED')),
    cancellation_date TIMESTAMP,
    refund_amount DECIMAL(10, 2) DEFAULT 0.00
);

CREATE INDEX idx_bookings_pnr ON bookings(pnr);
CREATE INDEX idx_bookings_user ON bookings(user_id);
CREATE INDEX idx_bookings_flight ON bookings(flight_id);

CREATE TABLE passengers (
    passenger_id SERIAL PRIMARY KEY,
    booking_id INT NOT NULL REFERENCES bookings(booking_id) ON DELETE CASCADE,
    seat_id INT REFERENCES seats(seat_id) ON DELETE SET NULL,
    first_name VARCHAR(60) NOT NULL,
    last_name VARCHAR(60) NOT NULL,
    gender VARCHAR(20) NOT NULL,
    age INT NOT NULL CHECK (age >= 0),
    id_number VARCHAR(50),
    seat_number VARCHAR(10) NOT NULL
);

CREATE INDEX idx_passengers_booking ON passengers(booking_id);

CREATE TABLE payments (
    payment_id SERIAL PRIMARY KEY,
    booking_id INT NOT NULL REFERENCES bookings(booking_id) ON DELETE CASCADE,
    amount DECIMAL(10, 2) NOT NULL CHECK (amount >= 0),
    payment_method VARCHAR(50) NOT NULL,
    transaction_ref VARCHAR(64) UNIQUE NOT NULL,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' CHECK (payment_status IN ('SUCCESS', 'FAILED', 'REFUNDED')),
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payments_booking ON payments(booking_id);
CREATE INDEX idx_payments_tx ON payments(transaction_ref);
