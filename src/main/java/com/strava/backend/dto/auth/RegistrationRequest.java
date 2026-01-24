package com.strava.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.strava.backend.constants.ErrorMessage.*;

@Data
public class RegistrationRequest {

    @NotBlank(message = EMPTY_FIRST_NAME)
    private String firstName;

    @NotBlank(message = EMPTY_LAST_NAME)
    private String lastName;

    @NotBlank(message = EMPTY_EMAIL)
    @Email(message = INVALID_EMAIL)
    private String email;

    @NotBlank(message = EMPTY_PASSWORD)
    @Size(min = 8, message = PASSWORD_MIN_LENGTH)
    @Size(max = 50, message = PASSWORD_MAX_LENGTH)
    private String password;

    @NotBlank(message = "Please confirm your password")
    private String confirmPassword;
}
