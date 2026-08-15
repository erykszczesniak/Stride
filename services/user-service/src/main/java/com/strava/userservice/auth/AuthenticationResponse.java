package com.strava.userservice.auth;

import com.strava.userservice.user.UserResponse;

/**
 * Auth payload, mirrored from the monolith's {@code AuthenticationResponse}:
 * token pair plus the (counter-less) user object the frontend stores after sign-in.
 */
public record AuthenticationResponse(
        String accessToken,
        String refreshToken,
        UserResponse user
) {}
