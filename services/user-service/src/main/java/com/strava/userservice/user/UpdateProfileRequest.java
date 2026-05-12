package com.strava.userservice.user;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 500) String avatar,
        @Size(max = 1000) String bio,
        @Size(max = 100) String city,
        @Size(max = 100) String country,
        Double weight,
        Double height,
        Boolean isPrivate,
        Boolean hideFromSearch
) {}
