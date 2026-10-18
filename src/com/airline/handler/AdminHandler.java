package com.airline.handler;

import com.airline.dao.AdminDAO;
import com.airline.model.Booking;
import com.airline.model.Flight;
import com.airline.util.SecurityUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.*;

public class AdminHandler implements HttpHandler {

    private final AdminDAO adminDAO = new AdminDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        if ("OPTIONS".equals(method)) {
            HandlerHelper.sendCors(exchange);
            return;
        }

        // Enforce admin authorization
        SecurityUtil.SessionData adminSession = HandlerHelper.requireAuth(exchange, "admin");
        if (adminSession == null) return;

        try {
            if ("GET".equals(method) && "/api/admin/stats".equals(path)) {
                Map<String, Object> stats = adminDAO.getDashboardStats();
                Map<String, Object> resp = new HashMap<>();
                resp.put("stats", stats);
                HandlerHelper.sendJson(exchange, 200, resp);

            } else if ("GET".equals(method) && "/api/admin/flights".equals(path)) {
                Map<String, String> q = HandlerHelper.getQueryParams(exchange);
                String search = q.getOrDefault("search", "");
                String status = q.getOrDefault("status", "ALL");

                List<Flight> flights = adminDAO.getAllFlights(search, status);
                List<Map<String, Object>> list = new ArrayList<>();
                for (Flight f : flights) list.add(f.toMap());

                Map<String, Object> resp = new HashMap<>();
                resp.put("flights", list);
                HandlerHelper.sendJson(exchange, 200, resp);

            } else if ("POST".equals(method) && "/api/admin/flights/add".equals(path)) {
                Map<String, Object> body = HandlerHelper.readJsonBody(exchange);
                int flightId = adminDAO.addFlight(body);

                Map<String, Object> resp = new HashMap<>();
                resp.put("message", "Flight added successfully!");
                resp.put("flight_id", flightId);
                HandlerHelper.sendJson(exchange, 201, resp);

            } else if ("POST".equals(method) && "/api/admin/flights/update".equals(path)) {
                Map<String, Object> body = HandlerHelper.readJsonBody(exchange);
                int flightId = Integer.parseInt(String.valueOf(body.get("flight_id")));
                adminDAO.updateFlight(flightId, body);

                Map<String, Object> resp = new HashMap<>();
                resp.put("message", "Flight updated successfully!");
                HandlerHelper.sendJson(exchange, 200, resp);

            } else if ("POST".equals(method) && "/api/admin/flights/cancel".equals(path)) {
                Map<String, Object> body = HandlerHelper.readJsonBody(exchange);
                int flightId = Integer.parseInt(String.valueOf(body.get("flight_id")));
                adminDAO.cancelFlight(flightId);

                Map<String, Object> resp = new HashMap<>();
                resp.put("message", "Flight cancelled successfully!");
                HandlerHelper.sendJson(exchange, 200, resp);

            } else if ("GET".equals(method) && "/api/admin/bookings".equals(path)) {
                Map<String, String> q = HandlerHelper.getQueryParams(exchange);
                String search = q.getOrDefault("search", "");
                String status = q.getOrDefault("status", "ALL");

                List<Booking> bookings = adminDAO.getAllBookings(search, status);
                List<Map<String, Object>> list = new ArrayList<>();
                for (Booking b : bookings) list.add(b.toMap());

                Map<String, Object> resp = new HashMap<>();
                resp.put("bookings", list);
                HandlerHelper.sendJson(exchange, 200, resp);

            } else if ("GET".equals(method) && path.matches("^/api/admin/tracker/\\d+$")) {
                String[] parts = path.split("/");
                int flightId = Integer.parseInt(parts[4]);
                Map<String, Object> tracker = adminDAO.getFlightSeatTracker(flightId);
                if (tracker == null) {
                    Map<String, Object> err = new HashMap<>();
                    err.put("error", "Flight not found.");
                    HandlerHelper.sendJson(exchange, 404, err);
                    return;
                }
                HandlerHelper.sendJson(exchange, 200, tracker);

            } else if ("GET".equals(method) && "/api/admin/scheduling".equals(path)) {
                Map<String, String> q = HandlerHelper.getQueryParams(exchange);
                String algo = q.getOrDefault("algo", "FCFS").toUpperCase();
                int quantum = 2;
                try {
                    quantum = Integer.parseInt(q.getOrDefault("quantum", "2"));
                } catch (Exception ignored) {}

                List<com.airline.util.SchedulerSimulator.ProcessTask> defaultTasks = com.airline.util.SchedulerSimulator.getDefaultAirlineTasks();
                Map<String, Object> result;
                if ("RR".equals(algo) || "ROUND_ROBIN".equals(algo)) {
                    result = com.airline.util.SchedulerSimulator.scheduleRoundRobin(defaultTasks, quantum);
                } else {
                    result = com.airline.util.SchedulerSimulator.scheduleFCFS(defaultTasks);
                }
                HandlerHelper.sendJson(exchange, 200, result);

            } else {
                Map<String, Object> err = new HashMap<>();
                err.put("error", "Endpoint not found: " + method + " " + path);
                HandlerHelper.sendJson(exchange, 404, err);
            }
        } catch (IllegalArgumentException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            HandlerHelper.sendJson(exchange, 400, err);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Admin operation error: " + e.getMessage());
            HandlerHelper.sendJson(exchange, 500, err);
        }
    }
}
