package com.smartparking.smart_parking.controller;

import com.smartparking.smart_parking.entity.ParkingSlot;
import com.smartparking.smart_parking.repository.ParkingSlotRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/slots")
public class ParkingSlotController {

    private final ParkingSlotRepository parkingSlotRepository;

    public ParkingSlotController(
            ParkingSlotRepository parkingSlotRepository) {

        this.parkingSlotRepository = parkingSlotRepository;
    }


    // ================= GET ALL SLOTS =================

    @GetMapping
    public List<ParkingSlot> getAllSlots() {

        return parkingSlotRepository.findAll();
    }


    // ================= GET AVAILABLE SLOTS =================

    @GetMapping("/available")
    public List<ParkingSlot> getAvailableSlots() {

        return parkingSlotRepository.findAll()
                .stream()
                .filter(slot ->
                        "AVAILABLE".equalsIgnoreCase(slot.getStatus()))
                .toList();
    }


    // ================= BOOK SLOT =================

    @PutMapping("/{id}/book")
    @Transactional
    public ParkingSlot bookSlot(
            @PathVariable Long id) {

        ParkingSlot slot =
                parkingSlotRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Parking slot not found"));

        if ("OCCUPIED".equalsIgnoreCase(slot.getStatus())) {

            throw new RuntimeException(
                    "Parking slot is already occupied");
        }

        slot.setStatus("OCCUPIED");

        return parkingSlotRepository.save(slot);
    }


    // ================= RELEASE SLOT =================

    @PutMapping("/{id}/release")
    @Transactional
    public ParkingSlot releaseSlot(
            @PathVariable Long id) {

        ParkingSlot slot =
                parkingSlotRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Parking slot not found"));

        slot.setStatus("AVAILABLE");

        return parkingSlotRepository.save(slot);
    }


    // ================= ADD NEW SLOT =================

    @PostMapping
    public ParkingSlot addSlot(
            @RequestBody ParkingSlot slot) {

        if (slot.getSlotNumber() == null ||
                slot.getSlotNumber().trim().isEmpty()) {

            throw new RuntimeException(
                    "Slot number is required");
        }

        String slotNum = slot.getSlotNumber().trim().toUpperCase();
        if (!slotNum.matches("[A-Z0-9][A-Z0-9-]{0,19}")) {
            throw new IllegalArgumentException("Slot number must be 1-20 letters, numbers, or hyphens");
        }

        if (parkingSlotRepository.existsBySlotNumber(slotNum)) {
            throw new RuntimeException("Slot " + slotNum + " already exists");
        }

        slot.setSlotNumber(slotNum);
        slot.setStatus("AVAILABLE");

        return parkingSlotRepository.save(slot);
    }


    // ================= UPDATE SLOT =================

    @PutMapping("/{id}")
    @Transactional
    public ParkingSlot updateSlot(
            @PathVariable Long id,
            @RequestBody ParkingSlot updatedSlot) {

        ParkingSlot slot =
                parkingSlotRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Parking slot not found"));

        if (updatedSlot.getSlotNumber() != null &&
                !updatedSlot.getSlotNumber()
                        .trim()
                        .isEmpty()) {

            String slotNumber = updatedSlot.getSlotNumber().trim().toUpperCase();
            if (!slotNumber.matches("[A-Z0-9][A-Z0-9-]{0,19}")) {
                throw new IllegalArgumentException("Slot number must be 1-20 letters, numbers, or hyphens");
            }
            if (!slotNumber.equalsIgnoreCase(slot.getSlotNumber()) &&
                    parkingSlotRepository.existsBySlotNumber(slotNumber)) {
                throw new IllegalArgumentException("Slot " + slotNumber + " already exists");
            }
            slot.setSlotNumber(slotNumber);
        }

        if (updatedSlot.getStatus() != null) {

            String status = updatedSlot.getStatus().trim().toUpperCase();
            if (!"AVAILABLE".equals(status) && !"OCCUPIED".equals(status)) {
                throw new IllegalArgumentException("Status must be AVAILABLE or OCCUPIED");
            }
            slot.setStatus(status);
        }

        return parkingSlotRepository.save(slot);
    }


    // ================= DELETE SLOT =================

    @DeleteMapping("/{id}")
    public void deleteSlot(
            @PathVariable Long id) {

        if (!parkingSlotRepository.existsById(id)) {

            throw new RuntimeException(
                    "Parking slot not found");
        }

        parkingSlotRepository.deleteById(id);
    }

}
