package com.airline.dao;

import com.airline.model.Airport;
import com.airline.model.Flight;
import com.airline.model.Seat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FlightDAO {

    public List<Airport> getAllAirports() throws SQLException {
        List<Airport> list = new ArrayList<>();
        String sql = DBConnection.normalizeSql("SELECT airport_id, airport_code, airport_name, city, country FROM airports ORDER BY city ASC, airport_name ASC;");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new Airport(
                    rs.getInt("airport_id"),
                    rs.getString("airport_code"),
                    rs.getString("airport_name"),
                    rs.getString("city"),
                    rs.getString("country")
                ));
            }
        }
        return list;
    }

    public List<Flight> searchFlights(String source, String dest, String travelDate, int passengers) throws SQLException {
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
            "WHERE f.available_seats >= ? AND f.status IN ('SCHEDULED', 'ACTIVE') "
        );

        List<Object> params = new ArrayList<>();
        params.add(passengers);

        if (source != null && !source.trim().isEmpty()) {
            sql.append(" AND (LOWER(sa.airport_code) = LOWER(?) OR sa.airport_id = ?) ");
            params.add(source.trim());
            int srcId = 0;
            try { srcId = Integer.parseInt(source.trim()); } catch (Exception ignored) {}
            params.add(srcId);
        }

        if (dest != null && !dest.trim().isEmpty()) {
            sql.append(" AND (LOWER(da.airport_code) = LOWER(?) OR da.airport_id = ?) ");
            params.add(dest.trim());
            int dstId = 0;
            try { dstId = Integer.parseInt(dest.trim()); } catch (Exception ignored) {}
            params.add(dstId);
        }

        if (travelDate != null && !travelDate.trim().isEmpty()) {
            sql.append(" AND substr(f.departure_time, 1, 10) = ? ");
            params.add(travelDate.trim());
        }

        sql.append(" ORDER BY f.departure_time ASC;");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DBConnection.normalizeSql(sql.toString()))) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapFlight(rs));
                }
            }
        }
        return list;
    }

    public Flight getFlightById(int flightId) throws SQLException {
        String sql = DBConnection.normalizeSql(
            "SELECT f.flight_id, f.flight_number, f.airline_name, " +
            "       f.source_airport_id, sa.airport_code AS source_code, sa.city AS source_city, sa.airport_name AS source_name, " +
            "       f.destination_airport_id, da.airport_code AS dest_code, da.city AS dest_city, da.airport_name AS dest_name, " +
            "       f.departure_time, f.arrival_time, f.duration_minutes, " +
            "       f.fare, f.total_seats, f.available_seats, f.aircraft_model, f.status " +
            "FROM flights f " +
            "JOIN airports sa ON f.source_airport_id = sa.airport_id " +
            "JOIN airports da ON f.destination_airport_id = da.airport_id " +
            "WHERE f.flight_id = ?;"
        );

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, flightId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapFlight(rs);
                }
            }
        }
        return null;
    }

    public List<Seat> getFlightSeats(int flightId) throws SQLException {
        List<Seat> seats = new ArrayList<>();
        String sql = DBConnection.normalizeSql(
            "SELECT seat_id, flight_id, seat_number, seat_class, is_booked, price_multiplier, version " +
            "FROM seats WHERE flight_id = ? " +
            "ORDER BY length(seat_number) ASC, seat_number ASC;"
        );

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, flightId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Seat s = new Seat(
                        rs.getInt("seat_id"),
                        rs.getInt("flight_id"),
                        rs.getString("seat_number"),
                        rs.getString("seat_class"),
                        rs.getInt("is_booked") == 1,
                        rs.getDouble("price_multiplier")
                    );
                    s.setVersion(rs.getInt("version"));
                    seats.add(s);
                }
            }
        }
        return seats;
    }

    private Flight mapFlight(ResultSet rs) throws SQLException {
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
        return f;
    }
}
