package com.strava.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.strava.backend.constants.ErrorMessage.*;

@Data
public class PasswordResetRequest {

    @NotBlank(message = EMPTY_PASSWORD)
    @Size(min = 8, message = PASSWORD_MIN_LENGTH)
    @Size(max = 50, message = PASSWORD_MAX_LENGTH)
    private String password;

    @NotBlank(message = "Please confirm your password")
    private String confirmPassword;
}
