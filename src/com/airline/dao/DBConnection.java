package com.airline.dao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DBConnection {

    private static final String PG_HOST = System.getenv().getOrDefault("DB_HOST", "localhost");
    private static final String PG_PORT = System.getenv().getOrDefault("DB_PORT", "5432");
    private static final String PG_DB = System.getenv().getOrDefault("DB_NAME", "airline_db");
    private static final String PG_USER = System.getenv().getOrDefault("DB_USER", "postgres");
    private static final String PG_PASS = System.getenv().getOrDefault("DB_PASSWORD", "postgres");

    private static final String PG_URL = "jdbc:postgresql://" + PG_HOST + ":" + PG_PORT + "/" + PG_DB;
    private static final String SQLITE_URL = "jdbc:sqlite:airline_db.sqlite";

    private static boolean isPostgres = false;
    private static boolean initialized = false;

    public static synchronized void initialize() {
        if (initialized) return;

        try {
            Class.forName("org.postgresql.Driver");
            DriverManager.setLoginTimeout(3);
            try (Connection conn = DriverManager.getConnection(PG_URL, PG_USER, PG_PASS)) {
                isPostgres = true;
                System.out.println("[DB] Successfully connected to PostgreSQL at " + PG_HOST + ":" + PG_PORT + "/" + PG_DB);
                ensurePostgresSchema(conn);
                initialized = true;
                return;
            }
        } catch (Exception e) {
            System.out.println("[DB] PostgreSQL not reachable at " + PG_HOST + ":" + PG_PORT + " (" + e.getMessage() + ")");
        }

        try {
            Class.forName("org.sqlite.JDBC");
            try (Connection conn = DriverManager.getConnection(SQLITE_URL)) {
                isPostgres = false;
                System.out.println("[DB] Running on persistent relational database (airline_db.sqlite)");
                System.out.println("[DB] To switch to PostgreSQL: start PostgreSQL service on port 5432, run database/schema.sql, and restart.");
                ensureSqliteSchema(conn);
                initialized = true;
            }
        } catch (Exception e) {
            System.err.println("[DB FATAL] Could not initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean isPostgres() {
        return isPostgres;
    }

    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            initialize();
        }
        if (isPostgres) {
            return DriverManager.getConnection(PG_URL, PG_USER, PG_PASS);
        } else {
            Connection conn = DriverManager.getConnection(SQLITE_URL);
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON;");
            }
            return conn;
        }
    }

    public static String normalizeSql(String sql) {
        if (!isPostgres) {
            sql = sql.replace("CURRENT_TIMESTAMP", "datetime('now')");
            sql = sql.replace("::DATE", "");
            sql = sql.replace("::TEXT", "");
        }
        return sql;
    }

    private static void ensurePostgresSchema(Connection conn) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "users", new String[]{"TABLE"})) {
                if (rs.next()) {
                    System.out.println("[DB] PostgreSQL tables already exist.");
                    try (Statement st = conn.createStatement()) {
                        st.executeUpdate("UPDATE users SET name='System Administrator', email='ritesh@gmail.com', password_hash='201e81408f0c53f978733167d7de65e0cc4d717ef0db6814ad19c637a2626850', salt='somesaltforadmin', role='admin' WHERE email='admin@skywings.com'");
                        st.executeUpdate("UPDATE users SET name='Passenger One', email='passenger1@example.com', password_hash='abc23a6afa9b34205f472496f2ef3b60347849eb2397e35008cfdcdea7e42aa7', salt='somesaltforjohn', role='passenger' WHERE email='john.doe@example.com'");
                    }
                    return;
                }
            }
            System.out.println("[DB] Executing database/schema.sql and database/seed_data.sql on PostgreSQL...");
            executeSqlFile(conn, "database/schema.sql");
            executeSqlFile(conn, "database/seed_data.sql");
            System.out.println("[DB] PostgreSQL database initialized and seeded successfully.");
        } catch (Exception e) {
            System.err.println("[DB] Warning initializing PostgreSQL schema: " + e.getMessage());
        }
    }

    private static void executeSqlFile(Connection conn, String relativePath) {
        File file = new File(relativePath);
        if (!file.exists()) {
            file = new File("..", relativePath);
        }
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            try (Statement st = conn.createStatement()) {
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("--") || line.isEmpty()) continue;
                    sb.append(" ").append(line);
                    if (line.endsWith(";")) {
                        try {
                            st.execute(sb.toString());
                        } catch (Exception ignored) {}
                        sb.setLength(0);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[DB] Error executing " + relativePath + ": " + e.getMessage());
        }
    }

    private static void ensureSqliteSchema(Connection conn) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "users", new String[]{"TABLE"})) {
                if (rs.next()) {
                    try (Statement st = conn.createStatement()) {
                        st.executeUpdate("UPDATE users SET name='System Administrator', email='ritesh@gmail.com', password_hash='201e81408f0c53f978733167d7de65e0cc4d717ef0db6814ad19c637a2626850', salt='somesaltforadmin', role='admin' WHERE email='admin@skywings.com'");
                        st.executeUpdate("UPDATE users SET name='Passenger One', email='passenger1@example.com', password_hash='abc23a6afa9b34205f472496f2ef3b60347849eb2397e35008cfdcdea7e42aa7', salt='somesaltforjohn', role='passenger' WHERE email='john.doe@example.com'");
                    }
                    return; // Already exists
                }
            }

            System.out.println("[DB] Creating local schema and seeding demo flights...");
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE users (" +
                        "user_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "name TEXT NOT NULL," +
                        "email TEXT UNIQUE NOT NULL," +
                        "password_hash TEXT NOT NULL," +
                        "salt TEXT NOT NULL," +
                        "role TEXT NOT NULL DEFAULT 'passenger'," +
                        "phone TEXT," +
                        "created_at DATETIME DEFAULT CURRENT_TIMESTAMP);");

                st.execute("CREATE TABLE airports (" +
                        "airport_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "airport_code TEXT UNIQUE NOT NULL," +
                        "airport_name TEXT NOT NULL," +
                        "city TEXT NOT NULL," +
                        "country TEXT NOT NULL);");

                st.execute("CREATE TABLE flights (" +
                        "flight_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "flight_number TEXT UNIQUE NOT NULL," +
                        "airline_name TEXT NOT NULL," +
                        "source_airport_id INTEGER NOT NULL REFERENCES airports(airport_id)," +
                        "destination_airport_id INTEGER NOT NULL REFERENCES airports(airport_id)," +
                        "departure_time DATETIME NOT NULL," +
                        "arrival_time DATETIME NOT NULL," +
                        "duration_minutes INTEGER NOT NULL," +
                        "fare REAL NOT NULL CHECK (fare > 0)," +
                        "total_seats INTEGER NOT NULL DEFAULT 56," +
                        "available_seats INTEGER NOT NULL DEFAULT 56," +
                        "aircraft_model TEXT DEFAULT 'Boeing 737-800'," +
                        "status TEXT NOT NULL DEFAULT 'SCHEDULED');");

                st.execute("CREATE TABLE seats (" +
                        "seat_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "flight_id INTEGER NOT NULL REFERENCES flights(flight_id) ON DELETE CASCADE," +
                        "seat_number TEXT NOT NULL," +
                        "seat_class TEXT NOT NULL DEFAULT 'ECONOMY'," +
                        "is_booked INTEGER NOT NULL DEFAULT 0," +
                        "price_multiplier REAL NOT NULL DEFAULT 1.0," +
                        "version INTEGER NOT NULL DEFAULT 0," +
                        "UNIQUE(flight_id, seat_number));");

                st.execute("CREATE TABLE bookings (" +
                        "booking_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "pnr TEXT UNIQUE NOT NULL," +
                        "user_id INTEGER NOT NULL REFERENCES users(user_id)," +
                        "flight_id INTEGER NOT NULL REFERENCES flights(flight_id)," +
                        "booking_date DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "passenger_count INTEGER NOT NULL DEFAULT 1," +
                        "total_fare REAL NOT NULL," +
                        "booking_status TEXT NOT NULL DEFAULT 'CONFIRMED'," +
                        "cancellation_date DATETIME," +
                        "refund_amount REAL DEFAULT 0.0);");

                st.execute("CREATE TABLE passengers (" +
                        "passenger_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "booking_id INTEGER NOT NULL REFERENCES bookings(booking_id) ON DELETE CASCADE," +
                        "seat_id INTEGER REFERENCES seats(seat_id)," +
                        "first_name TEXT NOT NULL," +
                        "last_name TEXT NOT NULL," +
                        "gender TEXT NOT NULL," +
                        "age INTEGER NOT NULL," +
                        "id_number TEXT," +
                        "seat_number TEXT NOT NULL);");

                st.execute("CREATE TABLE payments (" +
                        "payment_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "booking_id INTEGER NOT NULL REFERENCES bookings(booking_id) ON DELETE CASCADE," +
                        "amount REAL NOT NULL," +
                        "payment_method TEXT NOT NULL," +
                        "transaction_ref TEXT UNIQUE NOT NULL," +
                        "payment_status TEXT NOT NULL DEFAULT 'SUCCESS'," +
                        "payment_date DATETIME DEFAULT CURRENT_TIMESTAMP);");

                st.execute("INSERT INTO users (name, email, password_hash, salt, role, phone) VALUES " +
                        "('System Administrator', 'ritesh@gmail.com', '201e81408f0c53f978733167d7de65e0cc4d717ef0db6814ad19c637a2626850', 'somesaltforadmin', 'admin', '+1-800-555-0199')," +
                        "('Passenger One', 'passenger1@example.com', 'abc23a6afa9b34205f472496f2ef3b60347849eb2397e35008cfdcdea7e42aa7', 'somesaltforjohn', 'passenger', '+1-555-014-8822')," +
                        "('Jane Smith', 'jane.smith@example.com', 'f69a00b7195566968c2b2be5a4f4183b6076551f55f0f11e8d70a7f6996e1fe2', 'somesaltforjohn', 'passenger', '+1-555-019-3344');");

                st.execute("INSERT INTO airports (airport_code, airport_name, city, country) VALUES " +
                        "('DEL', 'Indira Gandhi International Airport', 'New Delhi', 'India')," +
                        "('BOM', 'Chhatrapati Shivaji Maharaj International Airport', 'Mumbai', 'India')," +
                        "('BLR', 'Kempegowda International Airport', 'Bengaluru', 'India')," +
                        "('DXB', 'Dubai International Airport', 'Dubai', 'United Arab Emirates')," +
                        "('LHR', 'Heathrow Airport', 'London', 'United Kingdom')," +
                        "('JFK', 'John F. Kennedy International Airport', 'New York', 'United States')," +
                        "('SIN', 'Singapore Changi Airport', 'Singapore', 'Singapore');");

                LocalDate today = LocalDate.now();
                String d0 = today.format(DateTimeFormatter.ISO_LOCAL_DATE);
                String d1 = today.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
                String d2 = today.plusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE);
                String d3 = today.plusDays(3).format(DateTimeFormatter.ISO_LOCAL_DATE);
                String d5 = today.plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE);

                String[][] flights = {
                    {"SW-101", "SkyWings Airways", "1", "2", d0 + " 06:00:00", d0 + " 08:15:00", "135", "4500.0", "56", "54", "Airbus A320neo", "SCHEDULED"},
                    {"SW-102", "SkyWings Airways", "1", "2", d0 + " 14:30:00", d0 + " 16:45:00", "135", "4950.0", "56", "56", "Boeing 737 MAX 8", "SCHEDULED"},
                    {"SW-103", "SkyWings Airways", "1", "2", d0 + " 20:15:00", d0 + " 22:30:00", "135", "4200.0", "56", "56", "Airbus A320neo", "SCHEDULED"},
                    {"SW-201", "SkyWings Airways", "2", "1", d0 + " 09:00:00", d0 + " 11:15:00", "135", "4600.0", "56", "56", "Airbus A320neo", "SCHEDULED"},
                    {"SW-202", "SkyWings Airways", "2", "1", d0 + " 18:00:00", d0 + " 20:15:00", "135", "5100.0", "56", "56", "Boeing 737 MAX 8", "SCHEDULED"},
                    {"SW-301", "SkyWings Airways", "1", "3", d1 + " 07:30:00", d1 + " 10:15:00", "165", "5400.0", "56", "56", "Boeing 737-800", "SCHEDULED"},
                    {"SW-302", "SkyWings Airways", "3", "1", d1 + " 17:00:00", d1 + " 19:45:00", "165", "5300.0", "56", "56", "Boeing 737-800", "SCHEDULED"},
                    {"SW-401", "SkyWings Airways", "2", "3", d1 + " 11:00:00", d1 + " 12:45:00", "105", "3800.0", "56", "56", "Airbus A320neo", "SCHEDULED"},
                    {"SW-501", "SkyWings International", "1", "4", d2 + " 04:15:00", d2 + " 06:45:00", "210", "18500.0", "56", "56", "Boeing 787-9 Dreamliner", "SCHEDULED"},
                    {"SW-502", "SkyWings International", "2", "4", d2 + " 13:00:00", d2 + " 15:15:00", "195", "17200.0", "56", "56", "Boeing 787-9 Dreamliner", "SCHEDULED"},
                    {"SW-601", "SkyWings International", "1", "5", d3 + " 02:00:00", d3 + " 07:30:00", "540", "42000.0", "56", "56", "Airbus A350-900", "SCHEDULED"},
                    {"SW-701", "SkyWings International", "1", "6", d5 + " 01:30:00", d5 + " 07:45:00", "885", "68000.0", "56", "56", "Boeing 777-300ER", "SCHEDULED"},
                    {"SW-801", "SkyWings International", "3", "7", d2 + " 08:30:00", d2 + " 15:15:00", "255", "14500.0", "56", "56", "Airbus A321neo", "SCHEDULED"}
                };

                for (String[] f : flights) {
                    st.execute(String.format("INSERT INTO flights (flight_number, airline_name, source_airport_id, destination_airport_id, departure_time, arrival_time, duration_minutes, fare, total_seats, available_seats, aircraft_model, status) VALUES ('%s', '%s', %s, %s, '%s', '%s', %s, %s, %s, %s, '%s', '%s')",
                            f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7], f[8], f[9], f[10], f[11]));
                }

                for (int fid = 1; fid <= flights.length; fid++) {
                    for (int r = 1; r <= 2; r++) {
                        for (String col : new String[]{"A", "B", "C", "D"}) {
                            st.execute(String.format("INSERT INTO seats (flight_id, seat_number, seat_class, is_booked, price_multiplier) VALUES (%d, '%d%s', 'BUSINESS', 0, 1.6)", fid, r, col));
                        }
                    }
                    for (int r = 3; r <= 10; r++) {
                        for (String col : new String[]{"A", "B", "C", "D", "E", "F"}) {
                            st.execute(String.format("INSERT INTO seats (flight_id, seat_number, seat_class, is_booked, price_multiplier) VALUES (%d, '%d%s', 'ECONOMY', 0, 1.0)", fid, r, col));
                        }
                    }
                }

                st.execute("UPDATE seats SET is_booked = 1 WHERE flight_id = 1 AND seat_number IN ('1A', '3A');");
                st.execute("INSERT INTO bookings (booking_id, pnr, user_id, flight_id, booking_date, passenger_count, total_fare, booking_status) " +
                        "VALUES (1, 'SW9K7X', 2, 1, datetime('now', '-2 days'), 2, 11700.0, 'CONFIRMED');");
                st.execute("INSERT INTO passengers (booking_id, first_name, last_name, gender, age, id_number, seat_number) " +
                        "VALUES (1, 'John', 'Doe', 'Male', 32, 'PASSPORT-A987654', '1A'), " +
                        "(1, 'Sarah', 'Doe', 'Female', 29, 'PASSPORT-A987655', '3A');");
                st.execute("INSERT INTO payments (booking_id, amount, payment_method, transaction_ref, payment_status, payment_date) " +
                        "VALUES (1, 11700.0, 'Credit Card (Simulated)', 'TXN-SW9K7X-88412', 'SUCCESS', datetime('now', '-2 days'));");

                System.out.println("[DB] Local database successfully populated with flights, seats, and demo bookings.");
            }
        } catch (Exception e) {
            System.err.println("[DB] Error ensuring SQLite schema: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
