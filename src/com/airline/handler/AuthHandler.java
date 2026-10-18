package com.airline.handler;

import com.airline.dao.UserDAO;
import com.airline.model.User;
import com.airline.util.SecurityUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AuthHandler implements HttpHandler {

    private final UserDAO userDAO = new UserDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        if ("OPTIONS".equals(method)) {
            HandlerHelper.sendCors(exchange);
            return;
        }

        try {
            if ("POST".equals(method) && path.endsWith("/register")) {
                handleRegister(exchange);
            } else if ("POST".equals(method) && path.endsWith("/login")) {
                handleLogin(exchange);
            } else if ("POST".equals(method) && path.endsWith("/logout")) {
                handleLogout(exchange);
            } else if ("GET".equals(method) && path.endsWith("/me")) {
                handleMe(exchange);
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
            err.put("error", "Internal server error: " + e.getMessage());
            HandlerHelper.sendJson(exchange, 500, err);
        }
    }

    private void handleRegister(HttpExchange exchange) throws Exception {
        Map<String, Object> body = HandlerHelper.readJsonBody(exchange);
        String name = String.valueOf(body.getOrDefault("name", ""));
        String email = String.valueOf(body.getOrDefault("email", ""));
        String password = String.valueOf(body.getOrDefault("password", ""));
        String phone = body.get("phone") != null ? String.valueOf(body.get("phone")) : "";

        User user = userDAO.register(name, email, password, phone);
        String token = SecurityUtil.createSession(user.getUserId(), user.getName(), user.getEmail(), user.getRole(), user.getPhone());

        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Registration successful!");
        Map<String, Object> uMap = user.toMap();
        uMap.put("token", token);
        resp.put("user", uMap);

        HandlerHelper.sendJson(exchange, 201, resp);
    }

    private void handleLogin(HttpExchange exchange) throws Exception {
        Map<String, Object> body = HandlerHelper.readJsonBody(exchange);
        String email = String.valueOf(body.getOrDefault("email", ""));
        String password = String.valueOf(body.getOrDefault("password", ""));
        String reqRole = body.get("required_role") != null ? String.valueOf(body.get("required_role")) : null;

        User user = userDAO.authenticate(email, password, reqRole);
        String token = SecurityUtil.createSession(user.getUserId(), user.getName(), user.getEmail(), user.getRole(), user.getPhone());

        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Login successful!");
        Map<String, Object> uMap = user.toMap();
        uMap.put("token", token);
        resp.put("user", uMap);

        HandlerHelper.sendJson(exchange, 200, resp);
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        String token = HandlerHelper.getAuthToken(exchange);
        if (token != null) {
            SecurityUtil.removeSession(token);
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Logged out successfully.");
        HandlerHelper.sendJson(exchange, 200, resp);
    }

    private void handleMe(HttpExchange exchange) throws IOException {
        SecurityUtil.SessionData session = HandlerHelper.requireAuth(exchange, null);
        if (session == null) return;

        Map<String, Object> uMap = new HashMap<>();
        uMap.put("user_id", session.userId);
        uMap.put("name", session.name);
        uMap.put("email", session.email);
        uMap.put("role", session.role);
        uMap.put("phone", session.phone);

        Map<String, Object> resp = new HashMap<>();
        resp.put("user", uMap);
        HandlerHelper.sendJson(exchange, 200, resp);
    }
}
