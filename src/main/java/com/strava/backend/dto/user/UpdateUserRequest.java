package com.strava.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import static com.strava.backend.constants.ErrorMessage.EMPTY_FIRST_NAME;
import static com.strava.backend.constants.ErrorMessage.EMPTY_LAST_NAME;

@Data
public class UpdateUserRequest {

    @NotBlank(message = EMPTY_FIRST_NAME)
    private String firstName;

    @NotBlank(message = EMPTY_LAST_NAME)
    private String lastName;

    private String bio;
    private String city;
    private String country;
}
