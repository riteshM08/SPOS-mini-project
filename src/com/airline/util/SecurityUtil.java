package com.airline.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Security and Session Utility
 * Handles PBKDF2-HMAC-SHA256 hashing, timing-safe verification, PNR generation,
 * and in-memory session tokens.
 */
public class SecurityUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int ITERATIONS = 100000;
    private static final int KEY_LENGTH = 256;

    // Session cache: token -> SessionData
    private static final Map<String, SessionData> SESSIONS = new ConcurrentHashMap<>();
    private static final long SESSION_EXPIRY_MS = 24L * 3600 * 1000; // 24 hours

    public static class SessionData {
        public final int userId;
        public final String name;
        public final String email;
        public final String role;
        public final String phone;
        public final long expiresAt;

        public SessionData(int userId, String name, String email, String role, String phone, long expiresAt) {
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.role = role;
            this.phone = phone;
            this.expiresAt = expiresAt;
        }
    }

    public static String generateSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return bytesToHex(salt);
    }

    public static String hashPassword(String password, String salt) {
        try {
            // Use salt as raw UTF-8 bytes — works for both plain-string salts (seed data)
            // and newly-generated hex salts (since hex chars are valid UTF-8).
            byte[] saltBytes = salt.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    public static boolean verifyPassword(String password, String storedHashHex, String saltHex) {
        String computedHashHex = hashPassword(password, saltHex);
        return MessageDigest.isEqual(computedHashHex.getBytes(), storedHashHex.getBytes());
    }

    public static String generatePNR() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder("SW");
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public static String generateToken() {
        byte[] tokenBytes = new byte[32];
        RANDOM.nextBytes(tokenBytes);
        return bytesToHex(tokenBytes);
    }

    public static String createSession(int userId, String name, String email, String role, String phone) {
        String token = generateToken();
        long expiresAt = System.currentTimeMillis() + SESSION_EXPIRY_MS;
        SESSIONS.put(token, new SessionData(userId, name, email, role, phone, expiresAt));
        return token;
    }

    public static SessionData getSession(String token) {
        if (token == null) return null;
        SessionData s = SESSIONS.get(token);
        if (s == null) return null;
        if (System.currentTimeMillis() > s.expiresAt) {
            SESSIONS.remove(token);
            return null;
        }
        return s;
    }

    public static void removeSession(String token) {
        if (token != null) {
            SESSIONS.remove(token);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }
}
