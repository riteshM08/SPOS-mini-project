package com.airline.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class Seat {
    private int seatId;
    private int flightId;
    private String seatNumber;
    private String seatClass;
    private boolean isBooked;
    private double priceMultiplier;
    private int version;

    public Seat() {}

    public Seat(int seatId, int flightId, String seatNumber, String seatClass, boolean isBooked, double priceMultiplier) {
        this.seatId = seatId;
        this.flightId = flightId;
        this.seatNumber = seatNumber;
        this.seatClass = seatClass;
        this.isBooked = isBooked;
        this.priceMultiplier = priceMultiplier;
    }

    public int getSeatId() { return seatId; }
    public void setSeatId(int seatId) { this.seatId = seatId; }
    public int getFlightId() { return flightId; }
    public void setFlightId(int flightId) { this.flightId = flightId; }
    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }
    public String getSeatClass() { return seatClass; }
    public void setSeatClass(String seatClass) { this.seatClass = seatClass; }
    public boolean isBooked() { return isBooked; }
    public void setBooked(boolean booked) { isBooked = booked; }
    public double getPriceMultiplier() { return priceMultiplier; }
    public void setPriceMultiplier(double priceMultiplier) { this.priceMultiplier = priceMultiplier; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("seat_id", seatId);
        m.put("flight_id", flightId);
        m.put("seat_number", seatNumber);
        m.put("seat_class", seatClass);
        m.put("is_booked", isBooked);
        m.put("price_multiplier", priceMultiplier);
        return m;
    }
}
