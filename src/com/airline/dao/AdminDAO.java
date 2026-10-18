package com.airline.dao;

import com.airline.model.Booking;
import com.airline.model.Flight;
import com.airline.model.Passenger;
import com.airline.model.Payment;
import com.airline.model.Seat;

import java.sql.*;
import java.util.*;

public class AdminDAO {

    public Map<String, Object> getDashboardStats() throws SQLException {
        Map<String, Object> stats = new LinkedHashMap<>();

        try (Connection conn = DBConnection.getConnection()) {
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(DBConnection.normalizeSql("SELECT COUNT(*) FROM flights;"))) {
                stats.put("total_flights", rs.next() ? rs.getInt(1) : 0);
            }

            String bSql = DBConnection.normalizeSql(
                "SELECT COUNT(*) AS total_bookings, " +
                "       SUM(CASE WHEN booking_status = 'CONFIRMED' THEN 1 ELSE 0 END) AS confirmed_bookings, " +
                "       SUM(CASE WHEN booking_status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelled_bookings, " +
                "       COALESCE(SUM(CASE WHEN booking_status = 'CONFIRMED' THEN total_fare ELSE 0 END), 0) AS total_revenue, " +
                "       COALESCE(SUM(passenger_count), 0) AS total_passengers " +
                "FROM bookings;"
            );
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(bSql)) {
                if (rs.next()) {
                    stats.put("total_bookings", rs.getInt("total_bookings"));
                    stats.put("confirmed_bookings", rs.getInt("confirmed_bookings"));
                    stats.put("cancelled_bookings", rs.getInt("cancelled_bookings"));
                    stats.put("total_revenue", rs.getDouble("total_revenue"));
                    stats.put("total_passengers", rs.getInt("total_passengers"));
                }
            }

            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(DBConnection.normalizeSql("SELECT COUNT(*) FROM users WHERE role = 'passenger';"))) {
                stats.put("total_users", rs.next() ? rs.getInt(1) : 0);
            }

            String occSql = DBConnection.normalizeSql("SELECT COALESCE(SUM(total_seats), 0) AS total_s, COALESCE(SUM(total_seats - available_seats), 0) AS booked_s FROM flights WHERE status IN ('SCHEDULED', 'ACTIVE');");
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(occSql)) {
                if (rs.next()) {
                    int totalS = rs.getInt("total_s");
                    int bookedS = rs.getInt("booked_s");
                    double occRate = totalS > 0 ? Math.round((double) bookedS / totalS * 1000.0) / 10.0 : 0.0;
                    stats.put("occupancy_rate", occRate);
                }
            }
        }

        return stats;
    }

    public List<Flight> getAllFlights(String search, String status) throws SQLException {
        List<Flight> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT f.flight_id, f.flight_number, f.airline_name, " +
            "       f.source_airport_id, sa.airport_code AS source_code, sa.city AS source_city, sa.airport_name AS source_name, " +
            "       f.destination_airport_id, da.airport_code AS dest_code, da.city AS dest_city, da.airport_name AS dest_name, " +
            "       f.departure_time, f.arrival_time, f.duration_minutes, " +
            "       f.fare, f.total_seats, f.available_seats, f.aircraft_model, f.status " +
            "FROM flights f " +
            "JOIN airports sa ON f.source_airport_id = sa.airport_id " +
            "JOIN airports da ON f.destination_airport_id = da.airport_id " +
            "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
            sql.append(" AND f.status = ? ");
            params.add(status.trim().toUpperCase());
        }

        if (search != null && !search.trim().isEmpty()) {
            String term = "%" + search.trim().toLowerCase() + "%";
            sql.append(" AND (LOWER(f.flight_number) LIKE ? OR LOWER(f.airline_name) LIKE ? OR LOWER(sa.city) LIKE ? OR LOWER(da.city) LIKE ?) ");
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
        }

        sql.append(" ORDER BY f.departure_time DESC;");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DBConnection.normalizeSql(sql.toString()))) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Flight f = new Flight();
                    f.setFlightId(rs.getInt("flight_id"));
                    f.setFlightNumber(rs.getString("flight_number"));
                    f.setAirlineName(rs.getString("airline_name"));
                    f.setSourceAirportId(rs.getInt("source_airport_id"));
                    f.setSourceCode(rs.getString("source_code"));
                    f.setSourceCity(rs.getString("source_city"));
                    f.setSourceName(rs.getString("source_name"));
                    f.setDestinationAirportId(rs.getInt("destination_airport_id"));
                    f.setDestCode(rs.getString("dest_code"));
                    f.setDestCity(rs.getString("dest_city"));
                    f.setDestName(rs.getString("dest_name"));
                    f.setDepartureTime(rs.getString("departure_time"));
                    f.setArrivalTime(rs.getString("arrival_time"));
                    f.setDurationMinutes(rs.getInt("duration_minutes"));
                    f.setFare(rs.getDouble("fare"));
                    f.setTotalSeats(rs.getInt("total_seats"));
                    f.setAvailableSeats(rs.getInt("available_seats"));
                    f.setAircraftModel(rs.getString("aircraft_model"));
                    f.setStatus(rs.getString("status"));
                    list.add(f);
                }
            }
        }

        return list;
    }

    public int addFlight(Map<String, Object> data) throws Exception {
        String flightNum = String.valueOf(data.get("flight_number")).trim().toUpperCase();
        String airline = data.get("airline_name") != null ? String.valueOf(data.get("airline_name")).trim() : "SkyWings Airways";
        int sourceId = Integer.parseInt(String.valueOf(data.get("source_airport_id")));
        int destId = Integer.parseInt(String.valueOf(data.get("destination_airport_id")));
        String depTime = String.valueOf(data.get("departure_time")).trim();
        String arrTime = String.valueOf(data.get("arrival_time")).trim();
        double fare = Double.parseDouble(String.valueOf(data.get("fare")));
        int totalSeats = data.get("total_seats") != null ? Integer.parseInt(String.valueOf(data.get("total_seats"))) : 56;
        String model = data.get("aircraft_model") != null ? String.valueOf(data.get("aircraft_model")).trim() : "Boeing 737-800";

        if (flightNum.isEmpty()) throw new IllegalArgumentException("Flight number is required.");
        if (sourceId == destId) throw new IllegalArgumentException("Source and destination must be different.");
        if (fare <= 0) throw new IllegalArgumentException("Fare must be positive.");

        int durationMinutes = 120; // default estimated
        try {
            if (depTime.length() >= 16 && arrTime.length() >= 16) {
                java.time.LocalDateTime d1 = java.time.LocalDateTime.parse(depTime.replace(" ", "T"));
                java.time.LocalDateTime d2 = java.time.LocalDateTime.parse(arrTime.replace(" ", "T"));
                durationMinutes = (int) java.time.Duration.between(d1, d2).toMinutes();
            }
        } catch (Exception ignored) {}

        Connection conn = DBConnection.getConnection();
        conn.setAutoCommit(false);

        try {
            String sql = DBConnection.normalizeSql(
                "INSERT INTO flights (flight_number, airline_name, source_airport_id, destination_airport_id, departure_time, arrival_time, duration_minutes, fare, total_seats, available_seats, aircraft_model, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SCHEDULED');"
            );

            int flightId = 0;
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, flightNum);
                stmt.setString(2, airline);
                stmt.setInt(3, sourceId);
                stmt.setInt(4, destId);
                stmt.setString(5, depTime);
                stmt.setString(6, arrTime);
                stmt.setInt(7, durationMinutes);
                stmt.setDouble(8, fare);
                stmt.setInt(9, totalSeats);
                stmt.setInt(10, totalSeats);
                stmt.setString(11, model);
                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) flightId = rs.getInt(1);
                }
            }

            if (flightId == 0) {
                try (PreparedStatement s2 = conn.prepareStatement(DBConnection.normalizeSql("SELECT flight_id FROM flights WHERE flight_number = ?;"))) {
                    s2.setString(1, flightNum);
                    try (ResultSet rs = s2.executeQuery()) {
                        if (rs.next()) flightId = rs.getInt(1);
                    }
                }
            }

            String seatSql = DBConnection.normalizeSql("INSERT INTO seats (flight_id, seat_number, seat_class, is_booked, price_multiplier) VALUES (?, ?, ?, 0, ?);");
            try (PreparedStatement stmt = conn.prepareStatement(seatSql)) {
                for (int r = 1; r <= 2; r++) {
                    for (String c : new String[]{"A", "B", "C", "D"}) {
                        stmt.setInt(1, flightId);
                        stmt.setString(2, r + c);
                        stmt.setString(3, "BUSINESS");
                        stmt.setDouble(4, 1.6);
                        stmt.executeUpdate();
                    }
                }
                for (int r = 3; r <= 10; r++) {
                    for (String c : new String[]{"A", "B", "C", "D", "E", "F"}) {
                        stmt.setInt(1, flightId);
                        stmt.setString(2, r + c);
                        stmt.setString(3, "ECONOMY");
                        stmt.setDouble(4, 1.0);
                        stmt.executeUpdate();
                    }
                }
            }

            conn.commit();
            conn.close();
            return flightId;

        } catch (Exception e) {
            conn.rollback();
            conn.close();
            throw e;
        }
    }

    public void updateFlight(int flightId, Map<String, Object> data) throws SQLException {
        StringBuilder sql = new StringBuilder("UPDATE flights SET status = ?");
        List<Object> params = new ArrayList<>();
        params.add(String.valueOf(data.getOrDefault("status", "SCHEDULED")).toUpperCase());

        if (data.containsKey("fare")) {
            sql.append(", fare = ?");
            params.add(Double.parseDouble(String.valueOf(data.get("fare"))));
        }
        if (data.containsKey("aircraft_model")) {
            sql.append(", aircraft_model = ?");
            params.add(String.valueOf(data.get("aircraft_model")));
        }
        if (data.containsKey("departure_time") && data.containsKey("arrival_time")) {
            sql.append(", departure_time = ?, arrival_time = ?");
            params.add(String.valueOf(data.get("departure_time")));
            params.add(String.valueOf(data.get("arrival_time")));
        }

        sql.append(" WHERE flight_id = ?;");
        params.add(flightId);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DBConnection.normalizeSql(sql.toString()))) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            stmt.executeUpdate();
        }
    }

    public void cancelFlight(int flightId) throws SQLException {
        String sql = DBConnection.normalizeSql("UPDATE flights SET status = 'CANCELLED' WHERE flight_id = ?;");
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, flightId);
            stmt.executeUpdate();
        }
    }

    public List<Booking> getAllBookings(String search, String status) throws SQLException {
        List<Booking> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT b.booking_id, b.pnr, b.booking_date, b.passenger_count, b.total_fare, b.booking_status, b.refund_amount, " +
            "       u.user_id, u.name AS customer_name, u.email AS customer_email, u.phone AS customer_phone, " +
            "       f.flight_id, f.flight_number, f.airline_name, f.departure_time, f.arrival_time, " +
            "       sa.airport_code AS source_code, sa.city AS source_city, " +
            "       da.airport_code AS dest_code, da.city AS dest_city, " +
            "       p.payment_id, p.payment_method, p.transaction_ref, p.payment_status " +
            "FROM bookings b " +
            "JOIN users u ON b.user_id = u.user_id " +
            "JOIN flights f ON b.flight_id = f.flight_id " +
            "JOIN airports sa ON f.source_airport_id = sa.airport_id " +
            "JOIN airports da ON f.destination_airport_id = da.airport_id " +
            "LEFT JOIN payments p ON b.booking_id = p.booking_id " +
            "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
            sql.append(" AND b.booking_status = ? ");
            params.add(status.trim().toUpperCase());
        }

        if (search != null && !search.trim().isEmpty()) {
            String term = "%" + search.trim().toLowerCase() + "%";
            sql.append(" AND (LOWER(b.pnr) LIKE ? OR LOWER(u.name) LIKE ? OR LOWER(u.email) LIKE ? OR LOWER(f.flight_number) LIKE ?) ");
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
        }

        sql.append(" ORDER BY b.booking_date DESC;");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DBConnection.normalizeSql(sql.toString()))) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Booking b = new Booking();
                    b.setBookingId(rs.getInt("booking_id"));
                    b.setPnr(rs.getString("pnr"));
                    b.setBookingDate(rs.getString("booking_date"));
                    b.setPassengerCount(rs.getInt("passenger_count"));
                    b.setTotalFare(rs.getDouble("total_fare"));
                    b.setBookingStatus(rs.getString("booking_status"));
                    b.setRefundAmount(rs.getDouble("refund_amount"));
                    b.setCustomerName(rs.getString("customer_name"));
                    b.setCustomerEmail(rs.getString("customer_email"));
                    b.setCustomerPhone(rs.getString("customer_phone"));

                    Flight f = new Flight();
                    f.setFlightId(rs.getInt("flight_id"));
                    f.setFlightNumber(rs.getString("flight_number"));
                    f.setAirlineName(rs.getString("airline_name"));
                    f.setDepartureTime(rs.getString("departure_time"));
                    f.setArrivalTime(rs.getString("arrival_time"));
                    f.setSourceCode(rs.getString("source_code"));
                    f.setSourceCity(rs.getString("source_city"));
                    f.setDestCode(rs.getString("dest_code"));
                    f.setDestCity(rs.getString("dest_city"));
                    b.setFlight(f);

                    if (rs.getString("payment_id") != null) {
                        Payment pay = new Payment();
                        pay.setPaymentId(rs.getInt("payment_id"));
                        pay.setPaymentMethod(rs.getString("payment_method"));
                        pay.setTransactionRef(rs.getString("transaction_ref"));
                        pay.setPaymentStatus(rs.getString("payment_status"));
                        b.setPayment(pay);
                    }

                    String paxSql = DBConnection.normalizeSql("SELECT first_name, last_name, seat_number, gender, age FROM passengers WHERE booking_id = ?;");
                    try (PreparedStatement pStmt = conn.prepareStatement(paxSql)) {
                        pStmt.setInt(1, b.getBookingId());
                        try (ResultSet pRs = pStmt.executeQuery()) {
                            while (pRs.next()) {
                                Passenger p = new Passenger();
                                p.setFirstName(pRs.getString("first_name"));
                                p.setLastName(pRs.getString("last_name"));
                                p.setSeatNumber(pRs.getString("seat_number"));
                                p.setGender(pRs.getString("gender"));
                                p.setAge(pRs.getInt("age"));
                                b.getPassengers().add(p);
                            }
                        }
                    }

                    list.add(b);
                }
            }
        }
        return list;
    }

    public Map<String, Object> getFlightSeatTracker(int flightId) throws SQLException {
        FlightDAO fDao = new FlightDAO();
        Flight flight = fDao.getFlightById(flightId);
        if (flight == null) return null;

        List<Seat> seats = fDao.getFlightSeats(flightId);
        int booked = 0;
        for (Seat s : seats) {
            if (s.isBooked()) booked++;
        }

        Map<String, Object> tracker = new LinkedHashMap<>();
        tracker.put("flight", flight.toMap());
        tracker.put("total_seats", seats.size());
        tracker.put("booked_seats", booked);
        tracker.put("available_seats", seats.size() - booked);
        tracker.put("occupancy_pct", seats.size() > 0 ? Math.round((double) booked / seats.size() * 1000.0) / 10.0 : 0.0);

        List<Map<String, Object>> seatMaps = new ArrayList<>();
        for (Seat s : seats) {
            seatMaps.add(s.toMap());
        }
        tracker.put("seats", seatMaps);
        return tracker;
    }
}
