package com.strava.userservice.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static com.strava.userservice.common.ErrorMessages.*;

/** Registration payload, mirrored from the monolith's {@code RegistrationRequest}. */
public record RegistrationRequest(
        @NotBlank(message = EMPTY_FIRST_NAME) String firstName,
        @NotBlank(message = EMPTY_LAST_NAME) String lastName,
        @NotBlank(message = EMPTY_EMAIL) @Email(message = INVALID_EMAIL) String email,
        @NotBlank(message = EMPTY_PASSWORD)
        @Size(min = 8, message = PASSWORD_MIN_LENGTH)
        @Size(max = 50, message = PASSWORD_MAX_LENGTH)
        String password,
        @NotBlank(message = "Please confirm your password") String confirmPassword
) {}
