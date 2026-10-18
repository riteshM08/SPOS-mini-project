package com.airline;

import com.airline.dao.DBConnection;
import com.airline.handler.*;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class AirlineServer {

    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        String portEnv = System.getenv("PORT");
        if (portEnv != null) {
            try {
                port = Integer.parseInt(portEnv);
            } catch (NumberFormatException ignored) {}
        }
        DBConnection.initialize();
        String frontendDir = "frontend";
        File fDir = new File(frontendDir);
        if (!fDir.exists()) {
            fDir = new File("..", frontendDir);
        }
        if (!fDir.exists()) {
            System.err.println("[WARN] Frontend directory not found at " + frontendDir);
        } else {
            System.out.println("[SERVER] Serving frontend assets from: " + fDir.getAbsolutePath());
        }
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/api/auth", new AuthHandler());
            server.createContext("/api/airports", new FlightHandler());
            server.createContext("/api/flights", new FlightHandler());
            server.createContext("/api/bookings", new BookingHandler());
            server.createContext("/api/admin", new AdminHandler());
            server.createContext("/", new StaticFileHandler(fDir.getAbsolutePath()));
            server.setExecutor(Executors.newCachedThreadPool());

            server.start();

            System.out.println("Server started successfully.");
            System.out.println("Open http://localhost:" + port);
            System.out.println("Press Ctrl+C to stop the server.");

        } catch (Exception e) {
            System.err.println("[FATAL] Failed to start HTTP server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
