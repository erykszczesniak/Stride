package com.strava.userservice.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import static com.strava.userservice.common.ErrorMessages.*;

/** Login payload, mirrored from the monolith's {@code AuthenticationRequest}. */
public record AuthenticationRequest(
        @NotBlank(message = EMPTY_EMAIL) @Email(message = INVALID_EMAIL) String email,
        @NotBlank(message = EMPTY_PASSWORD) String password
) {}
