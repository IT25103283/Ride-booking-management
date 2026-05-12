package com.ridemanagement.controller;

import com.ridemanagement.model.PremiumStratosphereRide;
import com.ridemanagement.model.Ride;
import com.ridemanagement.model.StandardSkyRide;
import com.ridemanagement.service.DriverAssignmentService;
import com.ridemanagement.service.RideFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

@Controller
public class RideController {

    private final RideFileService rideFileService;
    private final DriverAssignmentService driverAssignmentService;

    @Autowired
    public RideController(RideFileService rideFileService,
                          DriverAssignmentService driverAssignmentService) {
        this.rideFileService        = rideFileService;
        this.driverAssignmentService = driverAssignmentService;
    }

    @GetMapping("/")
    public String home(Model model) {
        try {
            model.addAttribute("stats", rideFileService.getStats());
            model.addAttribute("recentRides", rideFileService.getAllRides().stream().limit(5).toList());
        } catch (IOException e) {
            model.addAttribute("error", "Unable to load data: " + e.getMessage());
        }
        return "index";
    }

    @GetMapping("/book")
    public String bookForm(Model model) {
        model.addAttribute("ride", new StandardSkyRide());
        return "book-ride";
    }

    @PostMapping("/book")
    public String processBooking(@RequestParam String passengerName,
                                 @RequestParam String pickupCoordinates,
                                 @RequestParam String dropoffCoordinates,
                                 @RequestParam String rideType,
                                 RedirectAttributes redirectAttributes) {
        try {
            Ride ride;
            if ("PREMIUM".equalsIgnoreCase(rideType)) {
                ride = new PremiumStratosphereRide();
            } else {
                ride = new StandardSkyRide();
            }
            ride.setPassengerName(passengerName);
            ride.setPickupCoordinates(pickupCoordinates);
            ride.setDropoffCoordinates(dropoffCoordinates);

            List<Ride> currentRides = rideFileService.getAllRides();
            String driver = driverAssignmentService.assignDriver(ride, currentRides);
            ride.setDriverAssigned(driver);

            rideFileService.saveRide(ride);

            if ("UNASSIGNED".equals(driver)) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Ride booked! All " + driverAssignmentService.getFleetDrivers().size()
                        + " drivers are currently busy. Your ride is queued and will be assigned automatically when a driver becomes free.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Ride booked successfully! Driver assigned: " + driver);
            }

        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Booking failed: " + e.getMessage());
        }
        return "redirect:/history";
    }

    @GetMapping("/history")
    public String rideHistory(Model model) {
        try {
            model.addAttribute("rides", rideFileService.getAllRides());
            model.addAttribute("stats", rideFileService.getStats());
        } catch (IOException e) {
            model.addAttribute("error", "Could not load history: " + e.getMessage());
            model.addAttribute("rides", List.of());
        }
        return "ride-history";
    }

    @GetMapping("/history/search")
    public String searchHistory(@RequestParam(required = false) String name, Model model) {
        try {
            List<Ride> rides = (name != null && !name.isBlank())
                    ? rideFileService.getRidesByPassenger(name)
                    : rideFileService.getAllRides();
            model.addAttribute("rides", rides);
            model.addAttribute("searchName", name);
            model.addAttribute("stats", rideFileService.getStats());
        } catch (IOException e) {
            model.addAttribute("error", "Search failed: " + e.getMessage());
            model.addAttribute("rides", List.of());
        }
        return "ride-history";
    }

    @GetMapping("/active")
    public String activeRides(Model model) {
        try {
            model.addAttribute("airborneRides", rideFileService.getActiveRides());
            model.addAttribute("pendingRides",  rideFileService.getPendingRides());
            model.addAttribute("stats",         rideFileService.getStats());
        } catch (IOException e) {
            model.addAttribute("error", "Could not load active rides: " + e.getMessage());
        }
        return "active-ride";
    }

    @GetMapping("/ride/{id}")
    public String rideDetail(@PathVariable String id, Model model) {
        try {
            rideFileService.getRideById(id).ifPresentOrElse(
                    ride -> model.addAttribute("ride", ride),
                    ()   -> model.addAttribute("error", "Ride not found: " + id)
            );
        } catch (IOException e) {
            model.addAttribute("error", "Error loading ride: " + e.getMessage());
        }
        return "ride-detail";
    }

    @PostMapping("/ride/status")
    public String updateStatus(@RequestParam String rideId,
                               @RequestParam String newStatus,
                               @RequestParam(defaultValue = "/active") String redirectTo,
                               RedirectAttributes redirectAttributes) {
        try {
            // Get the ride before updating so we know the freed driver
            String freedDriver = rideFileService.getRideById(rideId)
                    .map(Ride::getDriverAssigned)
                    .orElse(null);

            boolean updated = rideFileService.updateRideStatus(rideId, newStatus);

            if (updated) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Ride " + rideId + " status updated to " + newStatus + ".");

                // When a ride is LANDED (completed), auto-assign freed driver to next queued ride
                if ("LANDED".equalsIgnoreCase(newStatus)
                        && freedDriver != null
                        && !"UNASSIGNED".equalsIgnoreCase(freedDriver)) {

                    List<Ride> queue = rideFileService.getUnassignedPendingRides();
                    if (!queue.isEmpty()) {
                        Ride nextRide = queue.get(0);
                        rideFileService.assignDriverToRide(nextRide.getRideId(), freedDriver);
                        redirectAttributes.addFlashAttribute("successMessage",
                                "Ride " + rideId + " completed. Driver " + freedDriver
                                + " has been automatically assigned to queued ride " + nextRide.getRideId() + ".");
                    }
                }

            } else {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Ride " + rideId + " not found.");
            }
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Update failed: " + e.getMessage());
        }
        return "redirect:" + redirectTo;
    }

    @PostMapping("/ride/cancel")
    public String cancelRide(@RequestParam String rideId,
                             RedirectAttributes redirectAttributes) {
        try {
            // Get freed driver before cancelling
            String freedDriver = rideFileService.getRideById(rideId)
                    .map(Ride::getDriverAssigned)
                    .orElse(null);

            boolean removed = rideFileService.cancelRide(rideId);

            if (removed) {
                // If a driver was assigned to this cancelled ride, assign them to the next queued ride
                if (freedDriver != null && !"UNASSIGNED".equalsIgnoreCase(freedDriver)) {
                    List<Ride> queue = rideFileService.getUnassignedPendingRides();
                    if (!queue.isEmpty()) {
                        Ride nextRide = queue.get(0);
                        rideFileService.assignDriverToRide(nextRide.getRideId(), freedDriver);
                        redirectAttributes.addFlashAttribute("successMessage",
                                "Ride " + rideId + " cancelled. Driver " + freedDriver
                                + " automatically assigned to queued ride " + nextRide.getRideId() + ".");
                    } else {
                        redirectAttributes.addFlashAttribute("successMessage",
                                "Ride " + rideId + " has been cancelled.");
                    }
                } else {
                    redirectAttributes.addFlashAttribute("successMessage",
                            "Ride " + rideId + " has been cancelled.");
                }
            } else {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Ride " + rideId + " was not found.");
            }
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Cancellation failed: " + e.getMessage());
        }
        return "redirect:/history";
    }
}
