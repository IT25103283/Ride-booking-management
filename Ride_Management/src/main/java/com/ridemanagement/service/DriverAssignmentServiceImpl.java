package com.ridemanagement.service;

import com.ridemanagement.model.Ride;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DriverAssignmentServiceImpl implements DriverAssignmentService {

    private static final List<String> FLEET_DRIVERS = Arrays.asList(
            "Kumara",
            "Priyantha",
            "Arjuna",
            "Sunil",
            "Vikum",
            "Kalana",
            "Ravi"
    );

    @Override
    public String assignDriver(Ride ride, List<Ride> activeRides) {
        List<String> busyDrivers = activeRides.stream()
                .filter(r -> "AIRBORNE".equalsIgnoreCase(r.getStatus())
                          || "PENDING".equalsIgnoreCase(r.getStatus()))
                .map(Ride::getDriverAssigned)
                .filter(d -> d != null && !d.equalsIgnoreCase("UNASSIGNED"))
                .collect(Collectors.toList());

        return FLEET_DRIVERS.stream()
                .filter(d -> !busyDrivers.contains(d))
                .findFirst()
                .orElse("UNASSIGNED");
    }

    @Override
    public List<String> getFleetDrivers() {
        return FLEET_DRIVERS;
    }
}
