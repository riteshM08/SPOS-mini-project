package com.airline.handler;

import com.airline.dao.BookingDAO;
import com.airline.model.Booking;
import com.airline.util.SecurityUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.*;

public class BookingHandler implements HttpHandler {

    private final BookingDAO bookingDAO = new BookingDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        if ("OPTIONS".equals(method)) {
            HandlerHelper.sendCors(exchange);
            return;
        }

        try {
            if ("POST".equals(method) && "/api/bookings/create".equals(path)) {
                handleCreateBooking(exchange);
            } else if ("GET".equals(method) && "/api/bookings/my".equals(path)) {
                handleMyBookings(exchange);
            } else if ("POST".equals(method) && "/api/bookings/cancel".equals(path)) {
                handleCancelBooking(exchange);
            } else if ("GET".equals(method) && path.matches("^/api/bookings/[a-zA-Z0-9]+$")) {
                String[] parts = path.split("/");
                String pnr = parts[3];
                Booking b = bookingDAO.getBookingByPNR(pnr);
                if (b == null) {
                    Map<String, Object> err = new HashMap<>();
                    err.put("error", "No reservation found with reference " + pnr + ".");
                    HandlerHelper.sendJson(exchange, 404, err);
                    return;
                }
                Map<String, Object> resp = new HashMap<>();
                resp.put("booking", b.toMap());
                HandlerHelper.sendJson(exchange, 200, resp);
            } else {
                Map<String, Object> err = new HashMap<>();
                err.put("error", "Endpoint not found: " + method + " " + path);
                HandlerHelper.sendJson(exchange, 404, err);
            }
        } catch (IllegalArgumentException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            HandlerHelper.sendJson(exchange, 400, err);
        } catch (IllegalStateException e) {
            // 409 Conflict for double-booking/seat taken
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            HandlerHelper.sendJson(exchange, 409, err);
        } catch (SecurityException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            HandlerHelper.sendJson(exchange, 403, err);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Error processing reservation: " + e.getMessage());
            HandlerHelper.sendJson(exchange, 500, err);
        }
    }

    private void handleCreateBooking(HttpExchange exchange) throws Exception {
        SecurityUtil.SessionData session = HandlerHelper.requireAuth(exchange, null);
        if (session == null) return;

        Map<String, Object> body = HandlerHelper.readJsonBody(exchange);
        int flightId = Integer.parseInt(String.valueOf(body.get("flight_id")));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> passengers = (List<Map<String, Object>>) body.get("passengers");

        @SuppressWarnings("unchecked")
        Map<String, Object> payment = (Map<String, Object>) body.get("payment_details");

        Booking confirmed = bookingDAO.createBooking(session.userId, flightId, passengers, payment);

        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Reservation confirmed successfully!");
        resp.put("booking", confirmed.toMap());
        HandlerHelper.sendJson(exchange, 201, resp);
    }

    private void handleMyBookings(HttpExchange exchange) throws Exception {
        SecurityUtil.SessionData session = HandlerHelper.requireAuth(exchange, null);
        if (session == null) return;

        List<Booking> bookings = bookingDAO.getUserBookings(session.userId);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Booking b : bookings) list.add(b.toMap());

        Map<String, Object> resp = new HashMap<>();
        resp.put("bookings", list);
        HandlerHelper.sendJson(exchange, 200, resp);
    }

    private void handleCancelBooking(HttpExchange exchange) throws Exception {
        SecurityUtil.SessionData session = HandlerHelper.requireAuth(exchange, null);
        if (session == null) return;

        Map<String, Object> body = HandlerHelper.readJsonBody(exchange);
        String pnr = String.valueOf(body.get("pnr")).trim();
        boolean isAdmin = "admin".equalsIgnoreCase(session.role);

        bookingDAO.cancelBooking(pnr, session.userId, isAdmin);

        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Reservation " + pnr + " successfully cancelled and refund processed.");
        HandlerHelper.sendJson(exchange, 200, resp);
    }
}
