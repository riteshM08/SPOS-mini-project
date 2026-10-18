package com.airline.handler;

import com.airline.dao.FlightDAO;
import com.airline.model.Airport;
import com.airline.model.Flight;
import com.airline.model.Seat;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.*;

public class FlightHandler implements HttpHandler {

    private final FlightDAO flightDAO = new FlightDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        if ("OPTIONS".equals(method)) {
            HandlerHelper.sendCors(exchange);
            return;
        }

        try {
            if ("GET".equals(method) && "/api/airports".equals(path)) {
                List<Airport> airports = flightDAO.getAllAirports();
                List<Map<String, Object>> list = new ArrayList<>();
                for (Airport a : airports) list.add(a.toMap());

                Map<String, Object> resp = new HashMap<>();
                resp.put("airports", list);
                HandlerHelper.sendJson(exchange, 200, resp);

            } else if ("GET".equals(method) && "/api/flights/search".equals(path)) {
                Map<String, String> q = HandlerHelper.getQueryParams(exchange);
                String src = q.getOrDefault("source", "");
                String dst = q.getOrDefault("dest", "");
                String date = q.getOrDefault("date", "");
                int pax = 1;
                try {
                    pax = Integer.parseInt(q.getOrDefault("passengers", "1"));
                } catch (Exception ignored) {}

                List<Flight> flights = flightDAO.searchFlights(src, dst, date, pax);
                List<Map<String, Object>> list = new ArrayList<>();
                for (Flight f : flights) list.add(f.toMap());

                Map<String, Object> resp = new HashMap<>();
                resp.put("flights", list);
                resp.put("count", list.size());
                HandlerHelper.sendJson(exchange, 200, resp);

            } else if ("GET".equals(method) && path.matches("^/api/flights/\\d+/seats$")) {
                String[] parts = path.split("/");
                int flightId = Integer.parseInt(parts[3]);
                List<Seat> seats = flightDAO.getFlightSeats(flightId);
                List<Map<String, Object>> list = new ArrayList<>();
                for (Seat s : seats) list.add(s.toMap());

                Map<String, Object> resp = new HashMap<>();
                resp.put("flight_id", flightId);
                resp.put("seats", list);
                HandlerHelper.sendJson(exchange, 200, resp);

            } else if ("GET".equals(method) && path.matches("^/api/flights/\\d+$")) {
                String[] parts = path.split("/");
                int flightId = Integer.parseInt(parts[3]);
                Flight f = flightDAO.getFlightById(flightId);
                if (f == null) {
                    Map<String, Object> err = new HashMap<>();
                    err.put("error", "Flight not found.");
                    HandlerHelper.sendJson(exchange, 404, err);
                    return;
                }
                Map<String, Object> resp = new HashMap<>();
                resp.put("flight", f.toMap());
                HandlerHelper.sendJson(exchange, 200, resp);

            } else {
                Map<String, Object> err = new HashMap<>();
                err.put("error", "Endpoint not found: " + method + " " + path);
                HandlerHelper.sendJson(exchange, 404, err);
            }
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Error: " + e.getMessage());
            HandlerHelper.sendJson(exchange, 500, err);
        }
    }
}
