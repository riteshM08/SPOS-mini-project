package com.airline.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class Airport {
    private int airportId;
    private String airportCode;
    private String airportName;
    private String city;
    private String country;

    public Airport() {}

    public Airport(int airportId, String airportCode, String airportName, String city, String country) {
        this.airportId = airportId;
        this.airportCode = airportCode;
        this.airportName = airportName;
        this.city = city;
        this.country = country;
    }

    public int getAirportId() { return airportId; }
    public void setAirportId(int airportId) { this.airportId = airportId; }
    public String getAirportCode() { return airportCode; }
    public void setAirportCode(String airportCode) { this.airportCode = airportCode; }
    public String getAirportName() { return airportName; }
    public void setAirportName(String airportName) { this.airportName = airportName; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("airport_id", airportId);
        m.put("airport_code", airportCode);
        m.put("airport_name", airportName);
        m.put("city", city);
        m.put("country", country);
        return m;
    }
}
