package com.ridemanagement.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class Ride {

    private String rideId;
    private String passengerName;
    private String pickupCoordinates;
    private String dropoffCoordinates;
    private String driverAssigned;
    private String status;
    private String rideType;
    private double fare;
    private String bookingTime;

    public static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Ride() {}

    public Ride(String rideId, String passengerName, String pickupCoordinates,
                String dropoffCoordinates, String rideType) {
        this.rideId             = rideId;
        this.passengerName      = passengerName;
        this.pickupCoordinates  = pickupCoordinates;
        this.dropoffCoordinates = dropoffCoordinates;
        this.rideType           = rideType;
        this.status             = "PENDING";
        this.bookingTime        = LocalDateTime.now().format(FORMATTER);
    }

    public abstract double calculateFare(double distanceKm);

    public abstract String getRideDetails();

    public String toFileString() {
        return String.join("|",
                rideId,
                rideType,
                passengerName,
                pickupCoordinates,
                dropoffCoordinates,
                driverAssigned != null ? driverAssigned : "UNASSIGNED",
                status,
                String.format("%.2f", fare),
                bookingTime
        );
    }

    public static double estimateDistance(String from, String to) {
        try {
            String[] a = from.split(",");
            String[] b = to.split(",");
            double lat1 = Double.parseDouble(a[0].trim());
            double lon1 = Double.parseDouble(a[1].trim());
            double lat2 = Double.parseDouble(b[0].trim());
            double lon2 = Double.parseDouble(b[1].trim());
            double dLat = Math.abs(lat2 - lat1);
            double dLon = Math.abs(lon2 - lon1);
            return Math.sqrt(dLat * dLat + dLon * dLon) * 111.0;
        } catch (Exception e) {
            return 10.0;
        }
    }

    public String getRideId()                        { return rideId; }
    public void   setRideId(String rideId)           { this.rideId = rideId; }

    public String getPassengerName()                         { return passengerName; }
    public void   setPassengerName(String passengerName)     { this.passengerName = passengerName; }

    public String getPickupCoordinates()                           { return pickupCoordinates; }
    public void   setPickupCoordinates(String pickupCoordinates)   { this.pickupCoordinates = pickupCoordinates; }

    public String getDropoffCoordinates()                              { return dropoffCoordinates; }
    public void   setDropoffCoordinates(String dropoffCoordinates)     { this.dropoffCoordinates = dropoffCoordinates; }

    public String getDriverAssigned()                          { return driverAssigned; }
    public void   setDriverAssigned(String driverAssigned)     { this.driverAssigned = driverAssigned; }

    public String getStatus()              { return status; }
    public void   setStatus(String status) { this.status = status; }

    public String getRideType()                { return rideType; }
    public void   setRideType(String rideType) { this.rideType = rideType; }

    public double getFare()            { return fare; }
    public void   setFare(double fare) { this.fare = fare; }

    public String getBookingTime()                     { return bookingTime; }
    public void   setBookingTime(String bookingTime)   { this.bookingTime = bookingTime; }
}
