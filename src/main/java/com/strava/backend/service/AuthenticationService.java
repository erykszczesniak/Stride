package com.strava.backend.service;

import com.strava.backend.dto.auth.AuthenticationRequest;
import com.strava.backend.dto.auth.AuthenticationResponse;
import com.strava.backend.dto.auth.RegistrationRequest;

public interface AuthenticationService {

    AuthenticationResponse login(AuthenticationRequest request);

    AuthenticationResponse register(RegistrationRequest request);

    AuthenticationResponse refreshToken(String refreshToken);

    void forgotPassword(String email);

    void resetPassword(String code, String newPassword);
}
