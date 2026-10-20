package com.smartparking.smart_parking;

import com.smartparking.smart_parking.entity.ParkingSlot;
import com.smartparking.smart_parking.entity.User;
import com.smartparking.smart_parking.repository.ParkingSlotRepository;
import com.smartparking.smart_parking.repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ParkingSlotRepository parkingSlotRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public DataInitializer(
            ParkingSlotRepository parkingSlotRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email:admin@smartpark.com}") String adminEmail,
            @Value("${app.admin.password:}") String adminPassword) {

        this.parkingSlotRepository = parkingSlotRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail.trim().toLowerCase();
        this.adminPassword = adminPassword;
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

        if (!adminPassword.isBlank() && !userRepository.existsByEmail(adminEmail)) {

            User admin = new User();

            admin.setName("Administrator");
            admin.setEmail(adminEmail);

            admin.setPassword(
                    passwordEncoder.encode(adminPassword)
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