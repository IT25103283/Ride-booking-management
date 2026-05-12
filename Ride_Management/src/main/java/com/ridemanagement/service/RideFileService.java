package com.ridemanagement.service;

import com.ridemanagement.model.PremiumStratosphereRide;
import com.ridemanagement.model.Ride;
import com.ridemanagement.model.StandardSkyRide;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RideFileService {

    @Value("${rides.file.path}")
    private String ridesFilePath;

    private void ensureFileExists() throws IOException {
        Path path = Paths.get(ridesFilePath);
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
        }
    }

    private Ride parseLine(String line) {
        if (line == null || line.isBlank()) return null;
        String[] parts = line.split("\\|", -1);
        if (parts.length < 9) return null;

        String rideType = parts[1].trim();
        Ride ride = "PREMIUM".equalsIgnoreCase(rideType)
                ? new PremiumStratosphereRide()
                : new StandardSkyRide();

        ride.setRideId(parts[0].trim());
        ride.setRideType(parts[1].trim());
        ride.setPassengerName(parts[2].trim());
        ride.setPickupCoordinates(parts[3].trim());
        ride.setDropoffCoordinates(parts[4].trim());
        ride.setDriverAssigned(parts[5].trim());
        ride.setStatus(parts[6].trim());
        try { ride.setFare(Double.parseDouble(parts[7].trim())); }
        catch (NumberFormatException ignored) { ride.setFare(0.0); }
        ride.setBookingTime(parts[8].trim());

        return ride;
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    public void saveRide(Ride ride) throws IOException {
        ensureFileExists();
        if (ride.getRideId() == null || ride.getRideId().isBlank()) {
            ride.setRideId("RIDE-" + System.currentTimeMillis());
        }
        double distKm = Ride.estimateDistance(ride.getPickupCoordinates(), ride.getDropoffCoordinates());
        ride.setFare(ride.calculateFare(distKm));

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ridesFilePath, true))) {
            bw.write(ride.toFileString());
            bw.newLine();
        }
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    public List<Ride> getAllRides() throws IOException {
        ensureFileExists();
        List<String> lines = Files.readAllLines(Paths.get(ridesFilePath));
        List<Ride> rides = lines.stream()
                .map(this::parseLine)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Collections.reverse(rides);
        return rides;
    }

    public Optional<Ride> getRideById(String rideId) throws IOException {
        return getAllRides().stream()
                .filter(r -> r.getRideId().equalsIgnoreCase(rideId))
                .findFirst();
    }

    public List<Ride> getRidesByPassenger(String passengerName) throws IOException {
        return getAllRides().stream()
                .filter(r -> r.getPassengerName()
                        .toLowerCase()
                        .contains(passengerName.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Ride> getActiveRides() throws IOException {
        return getAllRides().stream()
                .filter(r -> "AIRBORNE".equalsIgnoreCase(r.getStatus()))
                .collect(Collectors.toList());
    }

    public List<Ride> getPendingRides() throws IOException {
        return getAllRides().stream()
                .filter(r -> "PENDING".equalsIgnoreCase(r.getStatus()))
                .collect(Collectors.toList());
    }

    /** Returns PENDING rides that have no driver assigned yet (waiting for a free driver). */
    public List<Ride> getUnassignedPendingRides() throws IOException {
        // Reverse again so oldest is first (getAllRides returns newest-first)
        List<Ride> all = getAllRides();
        Collections.reverse(all);
        return all.stream()
                .filter(r -> "PENDING".equalsIgnoreCase(r.getStatus())
                          && "UNASSIGNED".equalsIgnoreCase(r.getDriverAssigned()))
                .collect(Collectors.toList());
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    public boolean updateRideStatus(String rideId, String newStatus) throws IOException {
        ensureFileExists();
        List<String> lines = Files.readAllLines(Paths.get(ridesFilePath));
        boolean updated = false;
        List<String> updatedLines = new ArrayList<>();

        for (String line : lines) {
            Ride ride = parseLine(line);
            if (ride != null && ride.getRideId().equalsIgnoreCase(rideId)) {
                ride.setStatus(newStatus);
                updatedLines.add(ride.toFileString());
                updated = true;
            } else {
                updatedLines.add(line);
            }
        }

        if (updated) {
            Files.write(Paths.get(ridesFilePath), updatedLines);
        }
        return updated;
    }

    /** Assign a driver to an existing PENDING/UNASSIGNED ride. */
    public boolean assignDriverToRide(String rideId, String driverName) throws IOException {
        ensureFileExists();
        List<String> lines = Files.readAllLines(Paths.get(ridesFilePath));
        boolean updated = false;
        List<String> updatedLines = new ArrayList<>();

        for (String line : lines) {
            Ride ride = parseLine(line);
            if (ride != null && ride.getRideId().equalsIgnoreCase(rideId)) {
                ride.setDriverAssigned(driverName);
                updatedLines.add(ride.toFileString());
                updated = true;
            } else {
                updatedLines.add(line);
            }
        }

        if (updated) {
            Files.write(Paths.get(ridesFilePath), updatedLines);
        }
        return updated;
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    public boolean cancelRide(String rideId) throws IOException {
        ensureFileExists();
        List<String> lines = Files.readAllLines(Paths.get(ridesFilePath));
        List<String> remaining = new ArrayList<>();
        boolean removed = false;

        for (String line : lines) {
            Ride ride = parseLine(line);
            if (ride != null && ride.getRideId().equalsIgnoreCase(rideId)) {
                removed = true;
            } else {
                remaining.add(line);
            }
        }

        if (removed) {
            Files.write(Paths.get(ridesFilePath), remaining);
        }
        return removed;
    }

    // ── STATS ─────────────────────────────────────────────────────────────────

    public Map<String, Long> getStats() throws IOException {
        List<Ride> all = getAllRides();
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("total",      (long) all.size());
        stats.put("pending",    all.stream().filter(r -> "PENDING".equalsIgnoreCase(r.getStatus())).count());
        stats.put("airborne",   all.stream().filter(r -> "AIRBORNE".equalsIgnoreCase(r.getStatus())).count());
        stats.put("landed",     all.stream().filter(r -> "LANDED".equalsIgnoreCase(r.getStatus())).count());
        stats.put("premium",    all.stream().filter(r -> "PREMIUM".equalsIgnoreCase(r.getRideType())).count());
        stats.put("standard",   all.stream().filter(r -> "STANDARD".equalsIgnoreCase(r.getRideType())).count());
        stats.put("unassigned", all.stream().filter(r -> "PENDING".equalsIgnoreCase(r.getStatus())
                                                      && "UNASSIGNED".equalsIgnoreCase(r.getDriverAssigned())).count());
        return stats;
    }
}
