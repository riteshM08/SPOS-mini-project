package com.airline.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class Flight {
    private int flightId;
    private String flightNumber;
    private String airlineName;
    private int sourceAirportId;
    private String sourceCode;
    private String sourceCity;
    private String sourceName;
    private int destinationAirportId;
    private String destCode;
    private String destCity;
    private String destName;
    private String departureTime;
    private String arrivalTime;
    private int durationMinutes;
    private double fare;
    private int totalSeats;
    private int availableSeats;
    private String aircraftModel;
    private String status;

    public Flight() {}

    public int getFlightId() { return flightId; }
    public void setFlightId(int flightId) { this.flightId = flightId; }
    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }
    public String getAirlineName() { return airlineName; }
    public void setAirlineName(String airlineName) { this.airlineName = airlineName; }
    public int getSourceAirportId() { return sourceAirportId; }
    public void setSourceAirportId(int sourceAirportId) { this.sourceAirportId = sourceAirportId; }
    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }
    public String getSourceCity() { return sourceCity; }
    public void setSourceCity(String sourceCity) { this.sourceCity = sourceCity; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public int getDestinationAirportId() { return destinationAirportId; }
    public void setDestinationAirportId(int destinationAirportId) { this.destinationAirportId = destinationAirportId; }
    public String getDestCode() { return destCode; }
    public void setDestCode(String destCode) { this.destCode = destCode; }
    public String getDestCity() { return destCity; }
    public void setDestCity(String destCity) { this.destCity = destCity; }
    public String getDestName() { return destName; }
    public void setDestName(String destName) { this.destName = destName; }
    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }
    public String getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(String arrivalTime) { this.arrivalTime = arrivalTime; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public double getFare() { return fare; }
    public void setFare(double fare) { this.fare = fare; }
    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }
    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }
    public String getAircraftModel() { return aircraftModel; }
    public void setAircraftModel(String aircraftModel) { this.aircraftModel = aircraftModel; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("flight_id", flightId);
        m.put("flight_number", flightNumber);
        m.put("airline_name", airlineName);
        m.put("source_airport_id", sourceAirportId);
        m.put("source_code", sourceCode);
        m.put("source_city", sourceCity);
        m.put("source_name", sourceName);
        m.put("destination_airport_id", destinationAirportId);
        m.put("dest_code", destCode);
        m.put("dest_city", destCity);
        m.put("dest_name", destName);
        m.put("departure_time", departureTime);
        m.put("arrival_time", arrivalTime);
        m.put("duration_minutes", durationMinutes);
        m.put("fare", fare);
        m.put("total_seats", totalSeats);
        m.put("available_seats", availableSeats);
        m.put("aircraft_model", aircraftModel);
        m.put("status", status);
        m.put("booked_seats", totalSeats - availableSeats);
        m.put("occupancy_pct", totalSeats > 0 ? Math.round((double)(totalSeats - availableSeats) / totalSeats * 1000.0) / 10.0 : 0.0);
        return m;
    }
}
