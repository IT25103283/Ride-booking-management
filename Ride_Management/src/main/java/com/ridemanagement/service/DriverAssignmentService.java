package com.ridemanagement.service;

import com.ridemanagement.model.Ride;
import java.util.List;

public interface DriverAssignmentService {

    String assignDriver(Ride ride, List<Ride> activeRides);

    List<String> getFleetDrivers();
}
