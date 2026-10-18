
INSERT INTO users (name, email, password_hash, salt, role, phone) VALUES
('System Administrator', 'ritesh@gmail.com', '201e81408f0c53f978733167d7de65e0cc4d717ef0db6814ad19c637a2626850', 'somesaltforadmin', 'admin', '+1-800-555-0199'),
('Passenger One', 'passenger1@example.com', 'abc23a6afa9b34205f472496f2ef3b60347849eb2397e35008cfdcdea7e42aa7', 'somesaltforjohn', 'passenger', '+1-555-014-8822'),
('Jane Smith', 'jane.smith@example.com', 'f69a00b7195566968c2b2be5a4f4183b6076551f55f0f11e8d70a7f6996e1fe2', 'somesaltforjohn', 'passenger', '+1-555-019-3344');

INSERT INTO airports (airport_code, airport_name, city, country) VALUES
('DEL', 'Indira Gandhi International Airport', 'New Delhi', 'India'),
('BOM', 'Chhatrapati Shivaji Maharaj International Airport', 'Mumbai', 'India'),
('BLR', 'Kempegowda International Airport', 'Bengaluru', 'India'),
('DXB', 'Dubai International Airport', 'Dubai', 'United Arab Emirates'),
('LHR', 'Heathrow Airport', 'London', 'United Kingdom'),
('JFK', 'John F. Kennedy International Airport', 'New York', 'United States'),
('SIN', 'Singapore Changi Airport', 'Singapore', 'Singapore');

INSERT INTO flights (flight_number, airline_name, source_airport_id, destination_airport_id, departure_time, arrival_time, duration_minutes, fare, total_seats, available_seats, aircraft_model, status) VALUES
('SW-101', 'SkyWings Airways', 1, 2, CURRENT_DATE + TIME '06:00:00', CURRENT_DATE + TIME '08:15:00', 135, 4500.00, 56, 54, 'Airbus A320neo', 'SCHEDULED'),
('SW-102', 'SkyWings Airways', 1, 2, CURRENT_DATE + TIME '14:30:00', CURRENT_DATE + TIME '16:45:00', 135, 4950.00, 56, 56, 'Boeing 737 MAX 8', 'SCHEDULED'),
('SW-103', 'SkyWings Airways', 1, 2, CURRENT_DATE + TIME '20:15:00', CURRENT_DATE + TIME '22:30:00', 135, 4200.00, 56, 56, 'Airbus A320neo', 'SCHEDULED'),
('SW-201', 'SkyWings Airways', 2, 1, CURRENT_DATE + TIME '09:00:00', CURRENT_DATE + TIME '11:15:00', 135, 4600.00, 56, 56, 'Airbus A320neo', 'SCHEDULED'),
('SW-202', 'SkyWings Airways', 2, 1, CURRENT_DATE + TIME '18:00:00', CURRENT_DATE + TIME '20:15:00', 135, 5100.00, 56, 56, 'Boeing 737 MAX 8', 'SCHEDULED'),

('SW-301', 'SkyWings Airways', 1, 3, (CURRENT_DATE + INTERVAL '1 day') + TIME '07:30:00', (CURRENT_DATE + INTERVAL '1 day') + TIME '10:15:00', 165, 5400.00, 56, 56, 'Boeing 737-800', 'SCHEDULED'),
('SW-302', 'SkyWings Airways', 3, 1, (CURRENT_DATE + INTERVAL '1 day') + TIME '17:00:00', (CURRENT_DATE + INTERVAL '1 day') + TIME '19:45:00', 165, 5300.00, 56, 56, 'Boeing 737-800', 'SCHEDULED'),
('SW-401', 'SkyWings Airways', 2, 3, (CURRENT_DATE + INTERVAL '1 day') + TIME '11:00:00', (CURRENT_DATE + INTERVAL '1 day') + TIME '12:45:00', 105, 3800.00, 56, 56, 'Airbus A320neo', 'SCHEDULED'),

('SW-501', 'SkyWings International', 1, 4, (CURRENT_DATE + INTERVAL '2 day') + TIME '04:15:00', (CURRENT_DATE + INTERVAL '2 day') + TIME '06:45:00', 210, 18500.00, 56, 56, 'Boeing 787-9 Dreamliner', 'SCHEDULED'),
('SW-502', 'SkyWings International', 2, 4, (CURRENT_DATE + INTERVAL '2 day') + TIME '13:00:00', (CURRENT_DATE + INTERVAL '2 day') + TIME '15:15:00', 195, 17200.00, 56, 56, 'Boeing 787-9 Dreamliner', 'SCHEDULED'),
('SW-601', 'SkyWings International', 1, 5, (CURRENT_DATE + INTERVAL '3 day') + TIME '02:00:00', (CURRENT_DATE + INTERVAL '3 day') + TIME '07:30:00', 540, 42000.00, 56, 56, 'Airbus A350-900', 'SCHEDULED'),
('SW-701', 'SkyWings International', 1, 6, (CURRENT_DATE + INTERVAL '5 day') + TIME '01:30:00', (CURRENT_DATE + INTERVAL '5 day') + TIME '07:45:00', 885, 68000.00, 56, 56, 'Boeing 777-300ER', 'SCHEDULED'),
('SW-801', 'SkyWings International', 3, 7, (CURRENT_DATE + INTERVAL '2 day') + TIME '08:30:00', (CURRENT_DATE + INTERVAL '2 day') + TIME '15:15:00', 255, 14500.00, 56, 56, 'Airbus A321neo', 'SCHEDULED');

DO $$
DECLARE
    f RECORD;
    r INT;
    col CHAR(1);
BEGIN
    FOR f IN SELECT flight_id FROM flights LOOP
        FOR r IN 1..2 LOOP
            FOREACH col IN ARRAY ARRAY['A', 'B', 'C', 'D'] LOOP
                INSERT INTO seats (flight_id, seat_number, seat_class, is_booked, price_multiplier)
                VALUES (f.flight_id, r || col, 'BUSINESS', FALSE, 1.60)
                ON CONFLICT (flight_id, seat_number) DO NOTHING;
            END LOOP;
        END LOOP;

        FOR r IN 3..10 LOOP
            FOREACH col IN ARRAY ARRAY['A', 'B', 'C', 'D', 'E', 'F'] LOOP
                INSERT INTO seats (flight_id, seat_number, seat_class, is_booked, price_multiplier)
                VALUES (f.flight_id, r || col, 'ECONOMY', FALSE, 1.00)
                ON CONFLICT (flight_id, seat_number) DO NOTHING;
            END LOOP;
        END LOOP;
    END LOOP;
END $$;

UPDATE seats SET is_booked = TRUE WHERE flight_id = 1 AND seat_number IN ('1A', '3A');

INSERT INTO bookings (booking_id, pnr, user_id, flight_id, booking_date, passenger_count, total_fare, booking_status)
VALUES (1, 'SW9K7X', 2, 1, CURRENT_TIMESTAMP - INTERVAL '2 days', 2, 11700.00, 'CONFIRMED');

INSERT INTO passengers (booking_id, first_name, last_name, gender, age, id_number, seat_number)
VALUES 
(1, 'John', 'Doe', 'Male', 32, 'PASSPORT-A987654', '1A'),
(1, 'Sarah', 'Doe', 'Female', 29, 'PASSPORT-A987655', '3A');

INSERT INTO payments (booking_id, amount, payment_method, transaction_ref, payment_status, payment_date)
VALUES 
(1, 11700.00, 'Credit Card (Simulated)', 'TXN-SW9K7X-88412', 'SUCCESS', CURRENT_TIMESTAMP - INTERVAL '2 days');

SELECT setval(pg_get_serial_sequence('users', 'user_id'), coalesce(max(user_id), 0) + 1, false) FROM users;
SELECT setval(pg_get_serial_sequence('airports', 'airport_id'), coalesce(max(airport_id), 0) + 1, false) FROM airports;
SELECT setval(pg_get_serial_sequence('flights', 'flight_id'), coalesce(max(flight_id), 0) + 1, false) FROM flights;
SELECT setval(pg_get_serial_sequence('seats', 'seat_id'), coalesce(max(seat_id), 0) + 1, false) FROM seats;
SELECT setval(pg_get_serial_sequence('bookings', 'booking_id'), coalesce(max(booking_id), 0) + 1, false) FROM bookings;
SELECT setval(pg_get_serial_sequence('passengers', 'passenger_id'), coalesce(max(passenger_id), 0) + 1, false) FROM passengers;
SELECT setval(pg_get_serial_sequence('payments', 'payment_id'), coalesce(max(payment_id), 0) + 1, false) FROM payments;
