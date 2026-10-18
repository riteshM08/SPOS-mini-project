package com.airline.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class Passenger {
    private int passengerId;
    private int bookingId;
    private Integer seatId;
    private String firstName;
    private String lastName;
    private String gender;
    private int age;
    private String idNumber;
    private String seatNumber;

    public Passenger() {}

    public int getPassengerId() { return passengerId; }
    public void setPassengerId(int passengerId) { this.passengerId = passengerId; }
    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }
    public Integer getSeatId() { return seatId; }
    public void setSeatId(Integer seatId) { this.seatId = seatId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("passenger_id", passengerId);
        m.put("booking_id", bookingId);
        m.put("first_name", firstName);
        m.put("last_name", lastName);
        m.put("gender", gender);
        m.put("age", age);
        m.put("id_number", idNumber);
        m.put("seat_number", seatNumber);
        return m;
    }
}
