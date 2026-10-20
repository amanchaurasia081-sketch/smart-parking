package com.smartparking.smart_parking.repository;

import com.smartparking.smart_parking.entity.ParkingSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, Long> {

    Optional<ParkingSlot> findBySlotNumber(String slotNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ParkingSlot> findWithLockBySlotNumber(String slotNumber);

    boolean existsBySlotNumber(String slotNumber);
}
