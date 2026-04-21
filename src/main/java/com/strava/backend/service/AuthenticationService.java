package com.strava.backend.service;

import com.strava.backend.dto.auth.AuthenticationRequest;
import com.strava.backend.dto.auth.AuthenticationResponse;
import com.strava.backend.dto.auth.GoogleAuthRequest;
import com.strava.backend.dto.auth.RegistrationRequest;

public interface AuthenticationService {

    AuthenticationResponse login(AuthenticationRequest request);

    AuthenticationResponse register(RegistrationRequest request);

    AuthenticationResponse googleLogin(GoogleAuthRequest request);

    /**
     * Find-or-create a user from a Strava OAuth authorization code. Strava's default scope
     * does not return an email, so users are keyed by {@code (providerId, authProvider)}
     * and receive a placeholder email of {@code strava_<athleteId>@stride.local} on signup.
     */
    AuthenticationResponse stravaLogin(String code);

    AuthenticationResponse refreshToken(String refreshToken);

    void forgotPassword(String email);

    void resetPassword(String code, String newPassword);
}
