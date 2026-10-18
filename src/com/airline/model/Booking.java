package com.airline.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Booking {
    private int bookingId;
    private String pnr;
    private int userId;
    private int flightId;
    private String bookingDate;
    private int passengerCount;
    private double totalFare;
    private String bookingStatus;
    private String cancellationDate;
    private double refundAmount;

    // Associated flight details
    private Flight flight;
    // Associated passengers
    private List<Passenger> passengers = new ArrayList<>();
    // Associated payment
    private Payment payment;

    // User details for admin view
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    public Booking() {}

    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }
    public String getPnr() { return pnr; }
    public void setPnr(String pnr) { this.pnr = pnr; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getFlightId() { return flightId; }
    public void setFlightId(int flightId) { this.flightId = flightId; }
    public String getBookingDate() { return bookingDate; }
    public void setBookingDate(String bookingDate) { this.bookingDate = bookingDate; }
    public int getPassengerCount() { return passengerCount; }
    public void setPassengerCount(int passengerCount) { this.passengerCount = passengerCount; }
    public double getTotalFare() { return totalFare; }
    public void setTotalFare(double totalFare) { this.totalFare = totalFare; }
    public String getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }
    public String getCancellationDate() { return cancellationDate; }
    public void setCancellationDate(String cancellationDate) { this.cancellationDate = cancellationDate; }
    public double getRefundAmount() { return refundAmount; }
    public void setRefundAmount(double refundAmount) { this.refundAmount = refundAmount; }

    public Flight getFlight() { return flight; }
    public void setFlight(Flight flight) { this.flight = flight; }
    public List<Passenger> getPassengers() { return passengers; }
    public void setPassengers(List<Passenger> passengers) { this.passengers = passengers; }
    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("booking_id", bookingId);
        m.put("pnr", pnr);
        m.put("user_id", userId);
        m.put("flight_id", flightId);
        m.put("booking_date", bookingDate);
        m.put("passenger_count", passengerCount);
        m.put("total_fare", totalFare);
        m.put("booking_status", bookingStatus);
        m.put("cancellation_date", cancellationDate);
        m.put("refund_amount", refundAmount);

        if (customerName != null) m.put("customer_name", customerName);
        if (customerEmail != null) m.put("customer_email", customerEmail);
        if (customerPhone != null) m.put("customer_phone", customerPhone);

        if (flight != null) {
            m.put("flight_number", flight.getFlightNumber());
            m.put("airline_name", flight.getAirlineName());
            m.put("departure_time", flight.getDepartureTime());
            m.put("arrival_time", flight.getArrivalTime());
            m.put("aircraft_model", flight.getAircraftModel());
            m.put("source_code", flight.getSourceCode());
            m.put("source_city", flight.getSourceCity());
            m.put("source_name", flight.getSourceName());
            m.put("dest_code", flight.getDestCode());
            m.put("dest_city", flight.getDestCity());
            m.put("dest_name", flight.getDestName());
        }

        List<Map<String, Object>> paxList = new ArrayList<>();
        List<String> seatList = new ArrayList<>();
        for (Passenger p : passengers) {
            paxList.add(p.toMap());
            seatList.add(p.getSeatNumber());
        }
        m.put("passengers", paxList);
        m.put("seats", String.join(", ", seatList));
        m.put("seats_list", seatList);

        if (payment != null) {
            m.put("payment_id", payment.getPaymentId());
            m.put("paid_amount", payment.getAmount());
            m.put("payment_method", payment.getPaymentMethod());
            m.put("transaction_ref", payment.getTransactionRef());
            m.put("payment_status", payment.getPaymentStatus());
            m.put("payment_date", payment.getPaymentDate());
        }

        return m;
    }
}
