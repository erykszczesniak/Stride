package com.strava.userservice.auth;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        Long userId,
        String email,
        String firstName,
        String lastName,
        String role
) {}
