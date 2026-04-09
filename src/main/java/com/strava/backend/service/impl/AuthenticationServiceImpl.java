package com.strava.backend.service.impl;

import com.strava.backend.domain.User;
import com.strava.backend.dto.auth.AuthenticationRequest;
import com.strava.backend.dto.auth.AuthenticationResponse;
import com.strava.backend.dto.auth.GoogleAuthRequest;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.strava.backend.constants.ErrorMessage.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final String GOOGLE_TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final AuthenticationManager authenticationManager;
    private final CommonMapper commonMapper;

    @Value("${google.client-id:}")
    private String googleClientId;

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
    @Transactional
    @SuppressWarnings("unchecked")
    public AuthenticationResponse googleLogin(GoogleAuthRequest request) {
        Map<String, Object> tokenInfo;
        try {
            RestTemplate restTemplate = new RestTemplate();
            tokenInfo = restTemplate.getForObject(
                    GOOGLE_TOKEN_INFO_URL + request.getIdToken(), Map.class);
        } catch (Exception e) {
            log.error("Failed to verify Google ID token", e);
            throw new ApiRequestException(INVALID_GOOGLE_TOKEN, HttpStatus.UNAUTHORIZED);
        }

        if (tokenInfo == null || tokenInfo.containsKey("error_description")) {
            throw new ApiRequestException(INVALID_GOOGLE_TOKEN, HttpStatus.UNAUTHORIZED);
        }

        String email = (String) tokenInfo.get("email");
        String firstName = (String) tokenInfo.get("given_name");
        String lastName = (String) tokenInfo.get("family_name");
        String providerId = (String) tokenInfo.get("sub");
        String avatar = (String) tokenInfo.get("picture");

        if (email == null || email.isBlank()) {
            throw new ApiRequestException(GOOGLE_AUTH_ERROR, HttpStatus.BAD_REQUEST);
        }

        Optional<User> existingUser = userRepository.findByEmail(email);

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            if (user.getAuthProvider() != AuthProvider.GOOGLE) {
                throw new ApiRequestException(GOOGLE_AUTH_EMAIL_MISMATCH, HttpStatus.CONFLICT);
            }
        } else {
            user = User.builder()
                    .firstName(firstName != null ? firstName : "")
                    .lastName(lastName != null ? lastName : "")
                    .email(email)
                    .authProvider(AuthProvider.GOOGLE)
                    .providerId(providerId)
                    .avatar(avatar)
                    .role(Role.USER)
                    .active(true)
                    .build();
            user = userRepository.save(user);
        }

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
