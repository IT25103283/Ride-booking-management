package com.ridemanagement.model;

public class PremiumStratosphereRide extends Ride {

    private static final double BASE_FARE        = 250.0;
    private static final double RATE_PER_KM      = 28.0;
    private static final double LUXURY_SURCHARGE = 150.0;

    public PremiumStratosphereRide() {
        super();
        setRideType("PREMIUM");
    }

    public PremiumStratosphereRide(String rideId, String passengerName,
                                   String pickupCoordinates, String dropoffCoordinates) {
        super(rideId, passengerName, pickupCoordinates, dropoffCoordinates, "PREMIUM");
    }

    @Override
    public double calculateFare(double distanceKm) {
        return BASE_FARE + (RATE_PER_KM * distanceKm) + LUXURY_SURCHARGE;
    }

    @Override
    public String getRideDetails() {
        return "Premium Ride - Luxury class. Base: Rs.250 + Rs.28/km + Rs.150 surcharge";
    }
}
