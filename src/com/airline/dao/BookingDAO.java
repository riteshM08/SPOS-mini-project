package com.airline.dao;

import com.airline.model.Booking;
import com.airline.model.Flight;
import com.airline.model.Passenger;
import com.airline.model.Payment;
import com.airline.util.SecurityUtil;

import java.sql.*;
import java.util.*;

public class BookingDAO {

    public Booking createBooking(int userId, int flightId, List<Map<String, Object>> paxDataList, Map<String, Object> paymentDetails) throws Exception {
        if (paxDataList == null || paxDataList.isEmpty()) {
            throw new IllegalArgumentException("At least one passenger is required for booking.");
        }

        Connection conn = DBConnection.getConnection();
        conn.setAutoCommit(false);

        try {
            String flightSql = DBConnection.normalizeSql("SELECT flight_id, flight_number, airline_name, fare, available_seats, status FROM flights WHERE flight_id = ?;");
            double baseFare = 0;
            int availableSeats = 0;
            String flightNum = "";
            String status = "";

            try (PreparedStatement stmt = conn.prepareStatement(flightSql)) {
                stmt.setInt(1, flightId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Selected flight does not exist.");
                    }
                    flightNum = rs.getString("flight_number");
                    baseFare = rs.getDouble("fare");
                    availableSeats = rs.getInt("available_seats");
                    status = rs.getString("status");
                }
            }

            if (!"SCHEDULED".equalsIgnoreCase(status) && !"ACTIVE".equalsIgnoreCase(status)) {
                throw new IllegalStateException("Flight " + flightNum + " is not open for booking (Status: " + status + ").");
            }

            if (availableSeats < paxDataList.size()) {
                throw new IllegalStateException("Only " + availableSeats + " seats remain on flight " + flightNum + ".");
            }

            Set<String> requestedSeats = new HashSet<>();
            for (Map<String, Object> p : paxDataList) {
                String sNum = String.valueOf(p.get("seat_number")).trim().toUpperCase();
                if (!requestedSeats.add(sNum)) {
                    throw new IllegalArgumentException("Duplicate seat '" + sNum + "' selected within the same reservation.");
                }
            }

            StringBuilder seatQuery = new StringBuilder("SELECT seat_id, seat_number, seat_class, is_booked, price_multiplier FROM seats WHERE flight_id = ? AND UPPER(seat_number) IN (");
            for (int i = 0; i < requestedSeats.size(); i++) {
                if (i > 0) seatQuery.append(",");
                seatQuery.append("?");
            }
            seatQuery.append(");");

            Map<String, Integer> seatIdMap = new HashMap<>();
            double totalCalculatedFare = 0;

            try (PreparedStatement stmt = conn.prepareStatement(DBConnection.normalizeSql(seatQuery.toString()))) {
                stmt.setInt(1, flightId);
                int pIdx = 2;
                for (String sNum : requestedSeats) {
                    stmt.setString(pIdx++, sNum);
                }
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int sId = rs.getInt("seat_id");
                        String sNum = rs.getString("seat_number").toUpperCase();
                        boolean isBooked = rs.getInt("is_booked") == 1;
                        double multiplier = rs.getDouble("price_multiplier");

                        if (isBooked) {
                            throw new IllegalStateException("Seat " + sNum + " has already been reserved by another passenger. Please select an available seat.");
                        }
                        seatIdMap.put(sNum, sId);
                        totalCalculatedFare += baseFare * multiplier;
                    }
                }
            }

            for (String sNum : requestedSeats) {
                if (!seatIdMap.containsKey(sNum)) {
                    throw new IllegalArgumentException("Seat " + sNum + " does not exist on flight " + flightNum + ".");
                }
            }

            String updateSeatSql = DBConnection.normalizeSql("UPDATE seats SET is_booked = 1, version = version + 1 WHERE seat_id = ? AND is_booked = 0;");
            try (PreparedStatement stmt = conn.prepareStatement(updateSeatSql)) {
                for (String sNum : requestedSeats) {
                    stmt.setInt(1, seatIdMap.get(sNum));
                    int affected = stmt.executeUpdate();
                    if (affected == 0) {
                        throw new IllegalStateException("Seat " + sNum + " was just booked by another user. Transaction cancelled.");
                    }
                }
            }

            String updateFlightSql = DBConnection.normalizeSql("UPDATE flights SET available_seats = available_seats - ? WHERE flight_id = ?;");
            try (PreparedStatement stmt = conn.prepareStatement(updateFlightSql)) {
                stmt.setInt(1, paxDataList.size());
                stmt.setInt(2, flightId);
                stmt.executeUpdate();
            }

            String pnr = SecurityUtil.generatePNR();
            String insertBookingSql = DBConnection.normalizeSql("INSERT INTO bookings (pnr, user_id, flight_id, passenger_count, total_fare, booking_status) VALUES (?, ?, ?, ?, ?, 'CONFIRMED');");
            int bookingId = 0;

            try (PreparedStatement stmt = conn.prepareStatement(insertBookingSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, pnr);
                stmt.setInt(2, userId);
                stmt.setInt(3, flightId);
                stmt.setInt(4, paxDataList.size());
                stmt.setDouble(5, totalCalculatedFare);
                stmt.executeUpdate();
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        bookingId = keys.getInt(1);
                    }
                }
            }

            if (bookingId == 0) {
                try (PreparedStatement s2 = conn.prepareStatement(DBConnection.normalizeSql("SELECT booking_id FROM bookings WHERE pnr = ?;"))) {
                    s2.setString(1, pnr);
                    try (ResultSet rs = s2.executeQuery()) {
                        if (rs.next()) bookingId = rs.getInt(1);
                    }
                }
            }

            String insertPaxSql = DBConnection.normalizeSql("INSERT INTO passengers (booking_id, seat_id, first_name, last_name, gender, age, id_number, seat_number) VALUES (?, ?, ?, ?, ?, ?, ?, ?);");
            try (PreparedStatement stmt = conn.prepareStatement(insertPaxSql)) {
                for (Map<String, Object> p : paxDataList) {
                    String sNum = String.valueOf(p.get("seat_number")).trim().toUpperCase();
                    int seatId = seatIdMap.get(sNum);

                    String fname = String.valueOf(p.get("first_name")).trim();
                    String lname = String.valueOf(p.get("last_name")).trim();
                    String gender = String.valueOf(p.getOrDefault("gender", "Other")).trim();
                    int age = 25;
                    try { age = Integer.parseInt(String.valueOf(p.get("age"))); } catch (Exception ignored) {}
                    String idNum = p.get("id_number") != null ? String.valueOf(p.get("id_number")).trim() : "";

                    stmt.setInt(1, bookingId);
                    stmt.setInt(2, seatId);
                    stmt.setString(3, fname);
                    stmt.setString(4, lname);
                    stmt.setString(5, gender);
                    stmt.setInt(6, age);
                    stmt.setString(7, idNum);
                    stmt.setString(8, sNum);
                    stmt.executeUpdate();
                }
            }

            String method = paymentDetails != null && paymentDetails.get("payment_method") != null 
                    ? String.valueOf(paymentDetails.get("payment_method")) 
                    : "Credit Card (Simulated)";
            String txnRef = "TXN-" + pnr + "-" + SecurityUtil.generatePNR().substring(2);

            String insertPaySql = DBConnection.normalizeSql("INSERT INTO payments (booking_id, amount, payment_method, transaction_ref, payment_status) VALUES (?, ?, ?, ?, 'SUCCESS');");
            try (PreparedStatement stmt = conn.prepareStatement(insertPaySql)) {
                stmt.setInt(1, bookingId);
                stmt.setDouble(2, totalCalculatedFare);
                stmt.setString(3, method);
                stmt.setString(4, txnRef);
                stmt.executeUpdate();
            }

            conn.commit();
            conn.close();

            return getBookingByPNR(pnr);

        } catch (Exception e) {
            conn.rollback();
            conn.close();
            throw e;
        }
    }

    public Booking getBookingByPNR(String pnr) throws SQLException {
        String sql = DBConnection.normalizeSql(
            "SELECT b.booking_id, b.pnr, b.user_id, b.flight_id, b.booking_date, " +
            "       b.passenger_count, b.total_fare, b.booking_status, b.cancellation_date, b.refund_amount, " +
            "       u.name AS customer_name, u.email AS customer_email, u.phone AS customer_phone, " +
            "       f.flight_number, f.airline_name, f.departure_time, f.arrival_time, f.duration_minutes, f.aircraft_model, " +
            "       sa.airport_code AS source_code, sa.city AS source_city, sa.airport_name AS source_name, " +
            "       da.airport_code AS dest_code, da.city AS dest_city, da.airport_name AS dest_name, " +
            "       p.payment_id, p.amount AS paid_amount, p.payment_method, p.transaction_ref, p.payment_status, p.payment_date " +
            "FROM bookings b " +
            "JOIN users u ON b.user_id = u.user_id " +
            "JOIN flights f ON b.flight_id = f.flight_id " +
            "JOIN airports sa ON f.source_airport_id = sa.airport_id " +
            "JOIN airports da ON f.destination_airport_id = da.airport_id " +
            "LEFT JOIN payments p ON b.booking_id = p.booking_id " +
            "WHERE UPPER(b.pnr) = UPPER(?);"
        );

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, pnr.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return null;

                Booking b = new Booking();
                b.setBookingId(rs.getInt("booking_id"));
                b.setPnr(rs.getString("pnr"));
                b.setUserId(rs.getInt("user_id"));
                b.setFlightId(rs.getInt("flight_id"));
                b.setBookingDate(rs.getString("booking_date"));
                b.setPassengerCount(rs.getInt("passenger_count"));
                b.setTotalFare(rs.getDouble("total_fare"));
                b.setBookingStatus(rs.getString("booking_status"));
                b.setCancellationDate(rs.getString("cancellation_date"));
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
                f.setDurationMinutes(rs.getInt("duration_minutes"));
                f.setAircraftModel(rs.getString("aircraft_model"));
                f.setSourceCode(rs.getString("source_code"));
                f.setSourceCity(rs.getString("source_city"));
                f.setSourceName(rs.getString("source_name"));
                f.setDestCode(rs.getString("dest_code"));
                f.setDestCity(rs.getString("dest_city"));
                f.setDestName(rs.getString("dest_name"));
                b.setFlight(f);

                if (rs.getString("payment_id") != null) {
                    Payment pay = new Payment();
                    pay.setPaymentId(rs.getInt("payment_id"));
                    pay.setAmount(rs.getDouble("paid_amount"));
                    pay.setPaymentMethod(rs.getString("payment_method"));
                    pay.setTransactionRef(rs.getString("transaction_ref"));
                    pay.setPaymentStatus(rs.getString("payment_status"));
                    pay.setPaymentDate(rs.getString("payment_date"));
                    b.setPayment(pay);
                }

                String paxSql = DBConnection.normalizeSql("SELECT passenger_id, booking_id, seat_id, first_name, last_name, gender, age, id_number, seat_number FROM passengers WHERE booking_id = ? ORDER BY passenger_id ASC;");
                try (PreparedStatement pStmt = conn.prepareStatement(paxSql)) {
                    pStmt.setInt(1, b.getBookingId());
                    try (ResultSet pRs = pStmt.executeQuery()) {
                        while (pRs.next()) {
                            Passenger p = new Passenger();
                            p.setPassengerId(pRs.getInt("passenger_id"));
                            p.setBookingId(pRs.getInt("booking_id"));
                            p.setSeatId(pRs.getInt("seat_id"));
                            p.setFirstName(pRs.getString("first_name"));
                            p.setLastName(pRs.getString("last_name"));
                            p.setGender(pRs.getString("gender"));
                            p.setAge(pRs.getInt("age"));
                            p.setIdNumber(pRs.getString("id_number"));
                            p.setSeatNumber(pRs.getString("seat_number"));
                            b.getPassengers().add(p);
                        }
                    }
                }

                return b;
            }
        }
    }

    public List<Booking> getUserBookings(int userId) throws SQLException {
        List<Booking> list = new ArrayList<>();
        String sql = DBConnection.normalizeSql(
            "SELECT b.booking_id, b.pnr, b.flight_id, b.booking_date, " +
            "       b.passenger_count, b.total_fare, b.booking_status, b.cancellation_date, " +
            "       f.flight_number, f.airline_name, f.departure_time, f.arrival_time, f.aircraft_model, " +
            "       sa.airport_code AS source_code, sa.city AS source_city, " +
            "       da.airport_code AS dest_code, da.city AS dest_city " +
            "FROM bookings b " +
            "JOIN flights f ON b.flight_id = f.flight_id " +
            "JOIN airports sa ON f.source_airport_id = sa.airport_id " +
            "JOIN airports da ON f.destination_airport_id = da.airport_id " +
            "WHERE b.user_id = ? " +
            "ORDER BY b.booking_date DESC;"
        );

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Booking b = new Booking();
                    b.setBookingId(rs.getInt("booking_id"));
                    b.setPnr(rs.getString("pnr"));
                    b.setFlightId(rs.getInt("flight_id"));
                    b.setBookingDate(rs.getString("booking_date"));
                    b.setPassengerCount(rs.getInt("passenger_count"));
                    b.setTotalFare(rs.getDouble("total_fare"));
                    b.setBookingStatus(rs.getString("booking_status"));
                    b.setCancellationDate(rs.getString("cancellation_date"));

                    Flight f = new Flight();
                    f.setFlightNumber(rs.getString("flight_number"));
                    f.setAirlineName(rs.getString("airline_name"));
                    f.setDepartureTime(rs.getString("departure_time"));
                    f.setArrivalTime(rs.getString("arrival_time"));
                    f.setAircraftModel(rs.getString("aircraft_model"));
                    f.setSourceCode(rs.getString("source_code"));
                    f.setSourceCity(rs.getString("source_city"));
                    f.setDestCode(rs.getString("dest_code"));
                    f.setDestCity(rs.getString("dest_city"));
                    b.setFlight(f);

                    String paxSql = DBConnection.normalizeSql("SELECT first_name, last_name, seat_number FROM passengers WHERE booking_id = ?;");
                    try (PreparedStatement pStmt = conn.prepareStatement(paxSql)) {
                        pStmt.setInt(1, b.getBookingId());
                        try (ResultSet pRs = pStmt.executeQuery()) {
                            while (pRs.next()) {
                                Passenger p = new Passenger();
                                p.setFirstName(pRs.getString("first_name"));
                                p.setLastName(pRs.getString("last_name"));
                                p.setSeatNumber(pRs.getString("seat_number"));
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

    public boolean cancelBooking(String pnr, Integer userId, boolean isAdmin) throws Exception {
        Connection conn = DBConnection.getConnection();
        conn.setAutoCommit(false);

        try {
            String checkSql = DBConnection.normalizeSql("SELECT booking_id, user_id, flight_id, total_fare, passenger_count, booking_status FROM bookings WHERE UPPER(pnr) = UPPER(?);");
            int bookingId = 0;
            int ownerId = 0;
            int flightId = 0;
            double fare = 0;
            int paxCount = 0;
            String status = "";

            try (PreparedStatement stmt = conn.prepareStatement(checkSql)) {
                stmt.setString(1, pnr.trim());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("No booking found with reference " + pnr + ".");
                    }
                    bookingId = rs.getInt("booking_id");
                    ownerId = rs.getInt("user_id");
                    flightId = rs.getInt("flight_id");
                    fare = rs.getDouble("total_fare");
                    paxCount = rs.getInt("passenger_count");
                    status = rs.getString("booking_status");
                }
            }

            if (!isAdmin && userId != null && ownerId != userId) {
                throw new SecurityException("You do not have permission to cancel this reservation.");
            }

            if ("CANCELLED".equalsIgnoreCase(status)) {
                throw new IllegalStateException("Reservation " + pnr + " has already been cancelled.");
            }

            List<Integer> seatIds = new ArrayList<>();
            String getSeatsSql = DBConnection.normalizeSql("SELECT seat_id FROM passengers WHERE booking_id = ? AND seat_id IS NOT NULL;");
            try (PreparedStatement stmt = conn.prepareStatement(getSeatsSql)) {
                stmt.setInt(1, bookingId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        seatIds.add(rs.getInt("seat_id"));
                    }
                }
            }

            if (!seatIds.isEmpty()) {
                StringBuilder releaseSql = new StringBuilder("UPDATE seats SET is_booked = 0 WHERE seat_id IN (");
                for (int i = 0; i < seatIds.size(); i++) {
                    if (i > 0) releaseSql.append(",");
                    releaseSql.append("?");
                }
                releaseSql.append(");");

                try (PreparedStatement stmt = conn.prepareStatement(DBConnection.normalizeSql(releaseSql.toString()))) {
                    for (int i = 0; i < seatIds.size(); i++) {
                        stmt.setInt(i + 1, seatIds.get(i));
                    }
                    stmt.executeUpdate();
                }
            }

            String returnSeatsSql = DBConnection.normalizeSql("UPDATE flights SET available_seats = available_seats + ? WHERE flight_id = ?;");
            try (PreparedStatement stmt = conn.prepareStatement(returnSeatsSql)) {
                stmt.setInt(1, paxCount);
                stmt.setInt(2, flightId);
                stmt.executeUpdate();
            }

            double refund = Math.round(fare * 0.90 * 100.0) / 100.0;
            String cancelBookingSql = DBConnection.normalizeSql("UPDATE bookings SET booking_status = 'CANCELLED', cancellation_date = CURRENT_TIMESTAMP, refund_amount = ? WHERE booking_id = ?;");
            try (PreparedStatement stmt = conn.prepareStatement(cancelBookingSql)) {
                stmt.setDouble(1, refund);
                stmt.setInt(2, bookingId);
                stmt.executeUpdate();
            }

            String refundPaySql = DBConnection.normalizeSql("UPDATE payments SET payment_status = 'REFUNDED' WHERE booking_id = ?;");
            try (PreparedStatement stmt = conn.prepareStatement(refundPaySql)) {
                stmt.setInt(1, bookingId);
                stmt.executeUpdate();
            }

            conn.commit();
            conn.close();
            return true;

        } catch (Exception e) {
            conn.rollback();
            conn.close();
            throw e;
        }
    }
}
