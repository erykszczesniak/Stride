package com.strava.userservice.user;

import jakarta.validation.constraints.NotBlank;

import static com.strava.userservice.common.ErrorMessages.EMPTY_FIRST_NAME;
import static com.strava.userservice.common.ErrorMessages.EMPTY_LAST_NAME;

/** Profile update payload, mirrored from the monolith's {@code UpdateUserRequest}. */
public record UpdateUserRequest(
        @NotBlank(message = EMPTY_FIRST_NAME) String firstName,
        @NotBlank(message = EMPTY_LAST_NAME) String lastName,
        String bio,
        String city,
        String country,
        Double weight,
        Double height,
        Boolean emailNotifications,
        Boolean pushNotifications,
        Boolean weeklySummary,
        Boolean isPrivate,
        Boolean hideFromSearch
) {}
