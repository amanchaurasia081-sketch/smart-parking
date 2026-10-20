package com.smartparking.smart_parking.controller;

import com.smartparking.smart_parking.entity.Booking;
import com.smartparking.smart_parking.entity.ParkingSlot;
import com.smartparking.smart_parking.repository.BookingRepository;
import com.smartparking.smart_parking.repository.ParkingSlotRepository;

import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private static final double HOURLY_RATE = 20.0;
    private static final ZoneId BUSINESS_TIME_ZONE = ZoneId.of("Asia/Kolkata");

    private final BookingRepository bookingRepository;
    private final ParkingSlotRepository parkingSlotRepository;

    public BookingController(
            BookingRepository bookingRepository,
            ParkingSlotRepository parkingSlotRepository) {

        this.bookingRepository = bookingRepository;
        this.parkingSlotRepository = parkingSlotRepository;
    }


    // ================= CREATE BOOKING =================

    @PostMapping
    @Transactional
    public Booking createBooking(
            @RequestBody Booking booking,
            Authentication authentication) {

        if (booking.getSlotNumber() == null ||
                booking.getSlotNumber().trim().isEmpty()) {

            throw new RuntimeException(
                    "Parking slot is required");
        }

        String vehicle = booking.getVehicle() == null ? "" : booking.getVehicle().trim().toUpperCase();
        if (!vehicle.matches("[A-Z0-9-]{2,20}")) {
            throw new IllegalArgumentException("Vehicle number must be 2-20 letters, numbers, or hyphens");
        }

        if (booking.getDuration() == null || booking.getDuration() < 1 || booking.getDuration() > 5) {
            throw new IllegalArgumentException("Duration must be between 1 and 5 hours");
        }

        if (booking.getBookingDate() == null || booking.getBookingDate().isBlank()) {
            throw new IllegalArgumentException("A valid booking date is required");
        }

        LocalDate bookingDate;
        try {
            bookingDate = LocalDate.parse(booking.getBookingDate());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("A valid booking date is required", exception);
        }
        if (bookingDate.isBefore(LocalDate.now(BUSINESS_TIME_ZONE))) {
            throw new IllegalArgumentException("Booking date cannot be in the past");
        }

        String slotNum = booking.getSlotNumber().trim().toUpperCase();
        booking.setSlotNumber(slotNum);
        booking.setUserEmail(authentication.getName());
        booking.setVehicle(vehicle);
        booking.setPrice(booking.getDuration() * HOURLY_RATE);
        booking.setStatus("active");
        booking.setPaymentStatus("Pending");
        booking.setEntryTime(null);
        booking.setExitTime(null);

        ParkingSlot parkingSlot =
                parkingSlotRepository.findWithLockBySlotNumber(slotNum)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Parking slot not found"));

        if ("OCCUPIED".equalsIgnoreCase(parkingSlot.getStatus())) {

            throw new RuntimeException(
                    "Parking slot is already occupied");
        }

        parkingSlot.setStatus("OCCUPIED");
        parkingSlotRepository.save(parkingSlot);

        return bookingRepository.save(booking);
    }


    // ================= GET USER BOOKINGS =================

    @GetMapping("/user/{email}")
    public List<Booking> getUserBookings(
            @PathVariable String email,
            Authentication authentication) {

        String requestedEmail = email.trim().toLowerCase();
        requireOwnerOrAdmin(requestedEmail, authentication);
        return bookingRepository.findByUserEmail(requestedEmail);
    }


    // ================= GET ALL BOOKINGS =================

    @GetMapping
    public List<Booking> getAllBookings() {

        return bookingRepository.findAll();
    }


    // ================= CANCEL BOOKING =================

    @PutMapping("/{id}/cancel")
    @Transactional
    public Booking cancelBooking(
            @PathVariable Long id,
            Authentication authentication) {

        Booking booking =
                bookingRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Booking not found"));
        requireOwnerOrAdmin(booking.getUserEmail(), authentication);

        if (!"active".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException(
                    "Only active bookings can be cancelled");
        }

        booking.setStatus("cancelled");

        releaseParkingSlot(
                booking.getSlotNumber());

        return bookingRepository.save(booking);
    }


    // ================= VEHICLE ENTRY =================

    @PutMapping("/{id}/entry")
    @Transactional
    public Booking vehicleEntry(
            @PathVariable Long id,
            Authentication authentication) {

        Booking booking =
                bookingRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Booking not found"));
        requireOwnerOrAdmin(booking.getUserEmail(), authentication);

        if ("cancelled".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException(
                    "Cancelled booking cannot be entered");
        }

        if (!"active".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException(
                    "Vehicle entry requires an active booking");
        }

        booking.setEntryTime(
                Instant.now().toString());

        booking.setStatus("parked");

        return bookingRepository.save(booking);
    }


    // ================= VEHICLE EXIT =================

    @PutMapping("/{id}/exit")
    @Transactional
    public Booking vehicleExit(
            @PathVariable Long id,
            Authentication authentication) {

        Booking booking =
                bookingRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Booking not found"));
        requireOwnerOrAdmin(booking.getUserEmail(), authentication);

        if ("cancelled".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException(
                    "Cancelled booking cannot exit");
        }

        if (!"parked".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException(
                    "Vehicle exit requires a parked booking");
        }

        booking.setExitTime(
                Instant.now().toString());

        booking.setStatus("completed");

        releaseParkingSlot(
                booking.getSlotNumber());

        return bookingRepository.save(booking);
    }


    // ================= VEHICLE EXIT BY SLOT NUMBER (ADMIN HELPER) =================

    @PutMapping("/slot/{slotNumber}/exit")
    @Transactional
    public Booking exitBySlotNumber(
            @PathVariable String slotNumber) {

        String cleanSlot = slotNumber.trim().toUpperCase();

        Booking activeBooking = bookingRepository.findAll().stream()
                .filter(b -> cleanSlot.equalsIgnoreCase(b.getSlotNumber()) &&
                        ("active".equalsIgnoreCase(b.getStatus()) || "parked".equalsIgnoreCase(b.getStatus())))
                .reduce((first, second) -> second)
                .orElse(null);

        if (activeBooking != null) {
            activeBooking.setExitTime(Instant.now().toString());
            activeBooking.setStatus("completed");
            bookingRepository.save(activeBooking);
        }

        releaseParkingSlot(cleanSlot);

        return activeBooking;
    }


    // ================= UPDATE PAYMENT =================

    @PutMapping("/{id}/payment")
    public Booking updatePayment(
            @PathVariable Long id,
            @RequestParam String status,
            Authentication authentication) {

        Booking booking =
                bookingRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Booking not found"));
        requireOwnerOrAdmin(booking.getUserEmail(), authentication);

        if (status == null ||
                status.trim().isEmpty()) {

            throw new RuntimeException(
                    "Payment status is required");
        }

        if (!"Pending".equalsIgnoreCase(status) && !"Paid".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Payment status must be Pending or Paid");
        }
        booking.setPaymentStatus(status.trim());

        return bookingRepository.save(booking);
    }


    // ================= DELETE BOOKING (ADMIN) =================

    @DeleteMapping("/{id}")
    @Transactional
    public void deleteBooking(@PathVariable Long id) {
        Booking booking = bookingRepository.findById(id).orElse(null);
        if (booking != null) {
            if ("active".equalsIgnoreCase(booking.getStatus()) || "parked".equalsIgnoreCase(booking.getStatus())) {
                releaseParkingSlot(booking.getSlotNumber());
            }
            bookingRepository.deleteById(id);
        }
    }


    // ================= RELEASE PARKING SLOT =================

    private void releaseParkingSlot(
            String slotNumber) {

        if (slotNumber == null) {
            return;
        }

        parkingSlotRepository.findBySlotNumber(slotNumber.trim().toUpperCase())
                .ifPresent(slot -> {
                    slot.setStatus("AVAILABLE");
                    parkingSlotRepository.save(slot);
                });
    }

    private void requireOwnerOrAdmin(String ownerEmail, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (!isAdmin && (ownerEmail == null ||
                !ownerEmail.equalsIgnoreCase(authentication.getName()))) {
            throw new org.springframework.security.access.AccessDeniedException("Not allowed to access this booking");
        }
    }

}
