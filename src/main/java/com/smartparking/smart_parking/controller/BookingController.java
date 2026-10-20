package com.smartparking.smart_parking.controller;

import com.smartparking.smart_parking.entity.Booking;
import com.smartparking.smart_parking.entity.ParkingSlot;
import com.smartparking.smart_parking.repository.BookingRepository;
import com.smartparking.smart_parking.repository.ParkingSlotRepository;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingController {

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
    public Booking createBooking(
            @RequestBody Booking booking) {

        if (booking.getSlotNumber() == null ||
                booking.getSlotNumber().trim().isEmpty()) {

            throw new RuntimeException(
                    "Parking slot is required");
        }

        if (booking.getUserEmail() == null ||
                booking.getUserEmail().trim().isEmpty()) {

            throw new RuntimeException(
                    "User email is required");
        }

        String slotNum = booking.getSlotNumber().trim().toUpperCase();
        booking.setSlotNumber(slotNum);

        ParkingSlot parkingSlot =
                parkingSlotRepository.findBySlotNumber(slotNum)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Parking slot not found"));

        if ("OCCUPIED".equalsIgnoreCase(parkingSlot.getStatus())) {

            throw new RuntimeException(
                    "Parking slot is already occupied");
        }

        if (booking.getStatus() == null) {
            booking.setStatus("active");
        }

        if (booking.getPaymentStatus() == null) {
            booking.setPaymentStatus("Pending");
        }

        parkingSlot.setStatus("OCCUPIED");
        parkingSlotRepository.save(parkingSlot);

        return bookingRepository.save(booking);
    }


    // ================= GET USER BOOKINGS =================

    @GetMapping("/user/{email}")
    public List<Booking> getUserBookings(
            @PathVariable String email) {

        return bookingRepository.findByUserEmail(email);
    }


    // ================= GET ALL BOOKINGS =================

    @GetMapping
    public List<Booking> getAllBookings() {

        return bookingRepository.findAll();
    }


    // ================= CANCEL BOOKING =================

    @PutMapping("/{id}/cancel")
    public Booking cancelBooking(
            @PathVariable Long id) {

        Booking booking =
                bookingRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Booking not found"));

        if ("cancelled".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException(
                    "Booking is already cancelled");
        }

        booking.setStatus("cancelled");

        releaseParkingSlot(
                booking.getSlotNumber());

        return bookingRepository.save(booking);
    }


    // ================= VEHICLE ENTRY =================

    @PutMapping("/{id}/entry")
    public Booking vehicleEntry(
            @PathVariable Long id) {

        Booking booking =
                bookingRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Booking not found"));

        if ("cancelled".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException(
                    "Cancelled booking cannot be entered");
        }

        if ("completed".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException(
                    "Booking is already completed");
        }

        booking.setEntryTime(
                LocalDateTime.now().toString());

        booking.setStatus("parked");

        return bookingRepository.save(booking);
    }


    // ================= VEHICLE EXIT =================

    @PutMapping("/{id}/exit")
    public Booking vehicleExit(
            @PathVariable Long id) {

        Booking booking =
                bookingRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Booking not found"));

        if ("cancelled".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException(
                    "Cancelled booking cannot exit");
        }

        if ("completed".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException(
                    "Vehicle has already exited");
        }

        booking.setExitTime(
                LocalDateTime.now().toString());

        booking.setStatus("completed");

        releaseParkingSlot(
                booking.getSlotNumber());

        return bookingRepository.save(booking);
    }


    // ================= UPDATE PAYMENT =================

    @PutMapping("/{id}/payment")
    public Booking updatePayment(
            @PathVariable Long id,
            @RequestParam String status) {

        Booking booking =
                bookingRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Booking not found"));

        if (status == null ||
                status.trim().isEmpty()) {

            throw new RuntimeException(
                    "Payment status is required");
        }

        booking.setPaymentStatus(status);

        return bookingRepository.save(booking);
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
}
