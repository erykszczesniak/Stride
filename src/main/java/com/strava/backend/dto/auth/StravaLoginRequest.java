package com.strava.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Payload for the Strava OAuth2 login flow. The frontend never constructs this directly —
 * it's built server-side by {@link com.strava.backend.controller.StravaAuthController} from
 * the {@code code} returned by Strava's redirect. Exposed as a DTO (rather than a raw
 * parameter) so the service contract stays consistent with {@link GoogleAuthRequest}.
 */
@Data
public class StravaLoginRequest {

    @NotBlank(message = "Strava authorization code is required")
    private String code;
}
