package com.airline.dao;

import com.airline.model.User;
import com.airline.util.SecurityUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class UserDAO {

    public User register(String name, String email, String password, String phone) throws Exception {
        email = email.trim().toLowerCase();
        name = name.trim();

        if (name.length() < 2) {
            throw new IllegalArgumentException("Full name must be at least 2 characters.");
        }
        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            throw new IllegalArgumentException("Please provide a valid email address.");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }

        String salt = SecurityUtil.generateSalt();
        String hash = SecurityUtil.hashPassword(password, salt);

        String sql = DBConnection.normalizeSql(
            "INSERT INTO users (name, email, password_hash, salt, role, phone) " +
            "VALUES (?, ?, ?, ?, 'passenger', ?);"
        );

        try (Connection conn = DBConnection.getConnection()) {
            // Check existing
            try (PreparedStatement checkStmt = conn.prepareStatement(DBConnection.normalizeSql("SELECT user_id FROM users WHERE LOWER(email) = LOWER(?);"))) {
                checkStmt.setString(1, email);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        throw new IllegalArgumentException("An account with this email address already exists. Please log in.");
                    }
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, name);
                stmt.setString(2, email);
                stmt.setString(3, hash);
                stmt.setString(4, salt);
                stmt.setString(5, phone != null ? phone.trim() : "");
                stmt.executeUpdate();

                int userId = 0;
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        userId = generatedKeys.getInt(1);
                    }
                }
                return new User(userId, name, email, "passenger", phone);
            }
        }
    }

    public User authenticate(String email, String password, String requiredRole) throws Exception {
        if (email == null || password == null || email.trim().isEmpty() || password.isEmpty()) {
            throw new IllegalArgumentException("Email and password are required.");
        }
        email = email.trim().toLowerCase();

        String sql = DBConnection.normalizeSql("SELECT user_id, name, email, password_hash, salt, role, phone FROM users WHERE LOWER(email) = LOWER(?);");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Invalid email or password.");
                }
                String storedHash = rs.getString("password_hash");
                String salt = rs.getString("salt");

                if (!SecurityUtil.verifyPassword(password, storedHash, salt)) {
                    throw new IllegalArgumentException("Invalid email or password.");
                }

                String role = rs.getString("role");
                if (requiredRole != null && !requiredRole.equalsIgnoreCase(role)) {
                    throw new IllegalArgumentException("Access denied. Account is not authorized as " + requiredRole + ".");
                }

                return new User(
                    rs.getInt("user_id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    role,
                    rs.getString("phone")
                );
            }
        }
    }

    public User getUserById(int userId) throws SQLException {
        String sql = DBConnection.normalizeSql("SELECT user_id, name, email, role, phone, created_at FROM users WHERE user_id = ?;");
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User u = new User(rs.getInt("user_id"), rs.getString("name"), rs.getString("email"), rs.getString("role"), rs.getString("phone"));
                    u.setCreatedAt(rs.getString("created_at"));
                    return u;
                }
            }
        }
        return null;
    }
}
