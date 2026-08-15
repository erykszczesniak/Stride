package com.strava.userservice.auth;

import jakarta.validation.constraints.NotBlank;

/** Google sign-in payload, mirrored from the monolith's {@code GoogleAuthRequest}. */
public record GoogleAuthRequest(
        @NotBlank(message = "Google ID token is required") String idToken
) {}
