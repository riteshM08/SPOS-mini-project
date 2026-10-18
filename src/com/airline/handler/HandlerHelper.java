package com.airline.handler;

import com.airline.util.JsonUtil;
import com.airline.util.SecurityUtil;
import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HandlerHelper {

    public static void sendJson(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = JsonUtil.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");

        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void sendCors(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(204, -1);
    }

    public static String readBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        String body = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        // Strip UTF-8 BOM (\uFEFF) if present — Windows curl/tools sometimes add it
        if (body.startsWith("\uFEFF")) {
            body = body.substring(1);
        }
        return body.trim();
    }

    public static Map<String, Object> readJsonBody(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        return JsonUtil.parseObject(body);
    }

    public static Map<String, String> getQueryParams(HttpExchange exchange) {
        Map<String, String> params = new HashMap<>();
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null || query.isEmpty()) return params;

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            try {
                if (idx > 0) {
                    params.put(
                        URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                        URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                    );
                } else if (idx == -1) {
                    params.put(URLDecoder.decode(pair, "UTF-8"), "");
                }
            } catch (Exception ignored) {}
        }
        return params;
    }

    public static String getAuthToken(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return null;
    }

    public static SecurityUtil.SessionData requireAuth(HttpExchange exchange, String requiredRole) throws IOException {
        String token = getAuthToken(exchange);
        if (token == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Authentication required. Please log in.");
            sendJson(exchange, 401, err);
            return null;
        }

        SecurityUtil.SessionData session = SecurityUtil.getSession(token);
        if (session == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Session expired or invalid. Please log in again.");
            sendJson(exchange, 401, err);
            return null;
        }

        if (requiredRole != null && !requiredRole.equalsIgnoreCase(session.role)) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Access denied. Action requires " + requiredRole + " role.");
            sendJson(exchange, 403, err);
            return null;
        }

        return session;
    }
}
