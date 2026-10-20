package com.smartparking.smart_parking.controller;

import com.smartparking.smart_parking.entity.User;
import com.smartparking.smart_parking.repository.UserRepository;
import com.smartparking.smart_parking.dto.AuthResponse;
import com.smartparking.smart_parking.security.TokenService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }


    // ================= REGISTER =================

    @PostMapping("/register")
    public AuthResponse register(@RequestBody User user) {

        String name = user.getName() == null ? "" : user.getName().trim();
        String email = user.getEmail() == null ? "" : user.getEmail().trim().toLowerCase();
        String password = user.getPassword();
        String vehicle = user.getVehicle() == null ? "" : user.getVehicle().trim().toUpperCase();

        if (name.isEmpty() || name.length() > 80) {
            throw new IllegalArgumentException("Name must be between 1 and 80 characters");
        }
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || email.length() > 254) {
            throw new IllegalArgumentException("A valid email address is required");
        }
        if (password == null || password.length() < 6 || password.length() > 72) {
            throw new IllegalArgumentException("Password must be between 6 and 72 characters");
        }
        if (!vehicle.matches("[A-Z0-9-]{2,20}")) {
            throw new IllegalArgumentException("Vehicle number must be 2-20 letters, numbers, or hyphens");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        user.setName(name);
        user.setEmail(email);
        user.setVehicle(vehicle);
        user.setPassword(
                passwordEncoder.encode(
                        password));

        user.setRole("USER");

        User savedUser = userRepository.save(user);
        return new AuthResponse(tokenService.issue(savedUser), savedUser);
    }


    // ================= LOGIN =================

    @PostMapping("/login")
    public AuthResponse login(@RequestBody User loginUser) {

        if (loginUser.getEmail() == null ||
                loginUser.getPassword() == null) {

            throw new RuntimeException(
                    "Email and password are required");
        }

        String email =
                loginUser.getEmail()
                        .trim()
                        .toLowerCase();

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Invalid email or password"));

        if (!passwordEncoder.matches(
                loginUser.getPassword(),
                user.getPassword())) {

            throw new RuntimeException(
                    "Invalid email or password");
        }

        return new AuthResponse(tokenService.issue(user), user);
    }
}