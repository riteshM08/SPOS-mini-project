package com.airline.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class User {
    private int userId;
    private String name;
    private String email;
    private String passwordHash;
    private String salt;
    private String role;
    private String phone;
    private String createdAt;

    public User() {}

    public User(int userId, String name, String email, String role, String phone) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.phone = phone;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("user_id", userId);
        m.put("name", name);
        m.put("email", email);
        m.put("role", role);
        m.put("phone", phone);
        m.put("created_at", createdAt);
        return m;
    }
}
