package com.smartparking.smart_parking.dto;

import com.smartparking.smart_parking.entity.User;

public record AuthResponse(String token, User user) {}
