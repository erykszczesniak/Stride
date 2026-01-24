package com.strava.backend.service.impl;

import com.strava.backend.domain.User;
import com.strava.backend.dto.auth.AuthenticationRequest;
import com.strava.backend.dto.auth.AuthenticationResponse;
import com.strava.backend.dto.auth.RegistrationRequest;
import com.strava.backend.dto.user.UserResponse;
import com.strava.backend.enums.AuthProvider;
import com.strava.backend.enums.Role;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.exception.EmailException;
import com.strava.backend.exception.PasswordException;
import com.strava.backend.mapper.CommonMapper;
import com.strava.backend.repository.UserRepository;
import com.strava.backend.security.UserPrincipal;
import com.strava.backend.security.jwt.JwtProvider;
import com.strava.backend.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.strava.backend.constants.ErrorMessage.*;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final AuthenticationManager authenticationManager;
    private final CommonMapper commonMapper;

    @Override
    public AuthenticationResponse login(AuthenticationRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));

        String accessToken = jwtProvider.generateToken(user.getId(), user.getEmail());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getEmail());

        return buildAuthenticationResponse(user, accessToken, refreshToken);
    }

    @Override
    @Transactional
    public AuthenticationResponse register(RegistrationRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailException(USER_ALREADY_EXISTS);
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new PasswordException(PASSWORDS_DO_NOT_MATCH);
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .authProvider(AuthProvider.LOCAL)
                .role(Role.USER)
                .active(true)
                .build();

        user = userRepository.save(user);

        String accessToken = jwtProvider.generateToken(user.getId(), user.getEmail());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getEmail());

        return buildAuthenticationResponse(user, accessToken, refreshToken);
    }

    @Override
    public AuthenticationResponse refreshToken(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new ApiRequestException(INVALID_JWT_TOKEN, HttpStatus.UNAUTHORIZED);
        }

        String email = jwtProvider.getEmailFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));

        String newAccessToken = jwtProvider.generateToken(user.getId(), user.getEmail());
        String newRefreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getEmail());

        return buildAuthenticationResponse(user, newAccessToken, newRefreshToken);
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiRequestException(EMAIL_NOT_FOUND, HttpStatus.NOT_FOUND));

        String resetCode = UUID.randomUUID().toString();
        user.setPasswordResetCode(resetCode);
        user.setPasswordResetCodeExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        // TODO: Send email with reset code
        // emailService.sendPasswordResetEmail(user.getEmail(), resetCode);
    }

    @Override
    @Transactional
    public void resetPassword(String code, String newPassword) {
        User user = userRepository.findByPasswordResetCode(code)
                .orElseThrow(() -> new ApiRequestException(INVALID_PASSWORD_CODE, HttpStatus.BAD_REQUEST));

        if (user.getPasswordResetCodeExpiry().isBefore(LocalDateTime.now())) {
            throw new ApiRequestException(PASSWORD_CODE_EXPIRED, HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetCode(null);
        user.setPasswordResetCodeExpiry(null);
        userRepository.save(user);
    }

    private AuthenticationResponse buildAuthenticationResponse(User user, String accessToken, String refreshToken) {
        UserResponse userResponse = commonMapper.convertToResponse(user, UserResponse.class);
        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userResponse)
                .build();
    }
}
