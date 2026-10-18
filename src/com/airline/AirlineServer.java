package com.airline;

import com.airline.dao.DBConnection;
import com.airline.handler.*;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Main Entry Point: Airline Reservation System (Java + JDBC + PostgreSQL)
 * Uses standard JDK HttpServer and JDBC connectivity with zero third-party web frameworks.
 */
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

        System.out.println("======================================================================");
        System.out.println("        AIRLINE RESERVATION SYSTEM (ARS) - JAVA BACKEND               ");
        System.out.println("        Technologies: Java 17 + JDBC + PostgreSQL Database            ");
        System.out.println("======================================================================");

        // 1. Initialize Database Connectivity & Schema
        DBConnection.initialize();

        // 2. Locate Frontend directory
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

        // 3. Create and configure HTTP Server
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

            // Register API Handlers
            server.createContext("/api/auth", new AuthHandler());
            server.createContext("/api/airports", new FlightHandler());
            server.createContext("/api/flights", new FlightHandler());
            server.createContext("/api/bookings", new BookingHandler());
            server.createContext("/api/admin", new AdminHandler());

            // Register Static Frontend File Handler
            server.createContext("/", new StaticFileHandler(fDir.getAbsolutePath()));

            // Multithreaded request handling pool
            server.setExecutor(Executors.newCachedThreadPool());

            server.start();

            System.out.println("\n✓ ARS Java Server started successfully!");
            System.out.println("✓ Local Web Application: http://localhost:" + port);
            System.out.println("✓ Default Passenger: john.doe@example.com  | Password: Password@123");
            System.out.println("✓ Default Admin:     admin@skywings.com    | Password: Admin@123");
            System.out.println("\n[Ready] Listening for incoming requests. Press Ctrl+C to terminate.\n" +
                               "======================================================================");

        } catch (Exception e) {
            System.err.println("[FATAL] Failed to start HTTP server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
