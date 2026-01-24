package com.strava.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import static com.strava.backend.constants.ErrorMessage.*;

@Data
public class AuthenticationRequest {

    @NotBlank(message = EMPTY_EMAIL)
    @Email(message = INVALID_EMAIL)
    private String email;

    @NotBlank(message = EMPTY_PASSWORD)
    private String password;
}
