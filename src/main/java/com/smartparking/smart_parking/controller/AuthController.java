package com.smartparking.smart_parking.controller;

import com.smartparking.smart_parking.entity.User;
import com.smartparking.smart_parking.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    // ================= REGISTER =================

    @PostMapping("/register")
    public User register(@RequestBody User user) {

        if (user.getName() == null ||
                user.getName().trim().isEmpty()) {

            throw new RuntimeException(
                    "Name is required");
        }

        if (user.getEmail() == null ||
                user.getEmail().trim().isEmpty()) {

            throw new RuntimeException(
                    "Email is required");
        }

        if (user.getPassword() == null ||
                user.getPassword().length() < 6) {

            throw new RuntimeException(
                    "Password must be at least 6 characters");
        }

        if (userRepository.existsByEmail(
                user.getEmail().trim().toLowerCase())) {

            throw new RuntimeException(
                    "Email is already registered");
        }

        user.setEmail(
                user.getEmail().trim().toLowerCase());

        user.setPassword(
                passwordEncoder.encode(
                        user.getPassword()));

        user.setRole("USER");

        return userRepository.save(user);
    }


    // ================= LOGIN =================

    @PostMapping("/login")
    public User login(@RequestBody User loginUser) {

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

        return user;
    }
}