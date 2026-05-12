package com.ridemanagement.model;

public class StandardSkyRide extends Ride {

    private static final double BASE_FARE   = 80.0;
    private static final double RATE_PER_KM = 12.0;

    public StandardSkyRide() {
        super();
        setRideType("STANDARD");
    }

    public StandardSkyRide(String rideId, String passengerName,
                           String pickupCoordinates, String dropoffCoordinates) {
        super(rideId, passengerName, pickupCoordinates, dropoffCoordinates, "STANDARD");
    }

    @Override
    public double calculateFare(double distanceKm) {
        return BASE_FARE + (RATE_PER_KM * distanceKm);
    }

    @Override
    public String getRideDetails() {
        return "Standard Ride - Economy class. Base: Rs.80 + Rs.12/km";
    }
}
