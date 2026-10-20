package com.smartparking.smart_parking;

import com.smartparking.smart_parking.entity.ParkingSlot;
import com.smartparking.smart_parking.entity.User;
import com.smartparking.smart_parking.repository.ParkingSlotRepository;
import com.smartparking.smart_parking.repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ParkingSlotRepository parkingSlotRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            ParkingSlotRepository parkingSlotRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.parkingSlotRepository = parkingSlotRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        // ================= PARKING SLOTS =================

        if (parkingSlotRepository.count() == 0) {

            for (int i = 1; i <= 20; i++) {

                String slotNumber =
                        String.format("A%02d", i);

                ParkingSlot slot =
                        new ParkingSlot(
                                slotNumber,
                                "AVAILABLE"
                        );

                parkingSlotRepository.save(slot);
            }

            System.out.println(
                    "20 parking slots created successfully!"
            );
        }

        // ================= ADMIN USER =================

        if (!userRepository.existsByEmail(
                "admin@smartpark.com")) {

            User admin = new User();

            admin.setName("Administrator");
            admin.setEmail("admin@smartpark.com");

            admin.setPassword(
                    passwordEncoder.encode("admin123")
            );

            admin.setRole("ADMIN");
            admin.setVehicle("");

            userRepository.save(admin);

            System.out.println(
                    "Admin user created successfully!"
            );
        }
    }
}