package com.strava.userservice.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.strava.userservice.domain.AuthProvider;
import com.strava.userservice.domain.Role;
import com.strava.userservice.domain.User;
import com.strava.userservice.domain.UserRepository;
import com.strava.userservice.event.UserEventPublisher;
import com.strava.userservice.exception.ApiRequestException;
import com.strava.userservice.user.UserResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.strava.userservice.common.ErrorMessages.*;

/**
 * Authentication flows mirrored from the monolith's {@code AuthenticationServiceImpl},
 * issuing this branch's gateway-compatible JWTs (userId/email/roles claims).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokens;
    private final UserEventPublisher events;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final StravaTokenExchanger stravaTokenExchanger;
    private final StravaProperties stravaProperties;

    @Transactional(readOnly = true)
    public AuthenticationResponse login(AuthenticationRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiRequestException(INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED));
        if (user.getPassword() == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ApiRequestException(INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED);
        }
        // Parity with the monolith: deactivated accounts can still log in — its
        // UserPrincipal.isEnabled() is hardcoded to true.
        return respond(user);
    }

    @Transactional
    public AuthenticationResponse register(RegistrationRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiRequestException(USER_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
        }
        if (!request.password().equals(request.confirmPassword())) {
            throw new ApiRequestException(PASSWORDS_DO_NOT_MATCH, HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.save(User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .authProvider(AuthProvider.LOCAL)
                .role(Role.USER)
                .active(true)
                .build());

        events.publishUserRegistered(user);
        return respond(user);
    }

    @Transactional
    public AuthenticationResponse googleLogin(GoogleAuthRequest request) {
        Map<String, Object> tokenInfo = googleTokenVerifier.verify(request.idToken());

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
            user = userRepository.save(User.builder()
                    .firstName(firstName != null ? firstName : "")
                    .lastName(lastName != null ? lastName : "")
                    .email(email)
                    .authProvider(AuthProvider.GOOGLE)
                    .providerId(providerId)
                    .avatar(avatar)
                    .role(Role.USER)
                    .active(true)
                    .build());
            events.publishUserRegistered(user);
        }

        return respond(user);
    }

    /**
     * Find-or-create a user from a Strava OAuth authorization code. Strava's default
     * scope does not return an email, so users are keyed by {@code (providerId,
     * authProvider)} and receive a placeholder email of
     * {@code strava_<athleteId>@stride.local} on signup.
     */
    @Transactional
    public AuthenticationResponse stravaLogin(String code) {
        // Exchange the authorization code for tokens + athlete summary. The exchanger wraps
        // network/parse failures in ApiRequestException so callers only need to handle that.
        JsonNode tokenJson = stravaTokenExchanger.exchange(code, stravaProperties.getLoginRedirectUri());

        JsonNode athlete = tokenJson.path("athlete");
        if (!athlete.hasNonNull("id")) {
            log.warn("Strava token response missing athlete.id: {}", tokenJson);
            throw new ApiRequestException(STRAVA_AUTH_ERROR, HttpStatus.BAD_GATEWAY);
        }

        String athleteId = athlete.get("id").asText();
        String firstName = athlete.hasNonNull("firstname") ? athlete.get("firstname").asText() : "";
        String lastName = athlete.hasNonNull("lastname") ? athlete.get("lastname").asText() : "";
        String avatar = athlete.hasNonNull("profile") ? athlete.get("profile").asText() : null;
        String city = athlete.hasNonNull("city") ? athlete.get("city").asText() : null;
        String country = athlete.hasNonNull("country") ? athlete.get("country").asText() : null;

        User user = userRepository
                .findByProviderIdAndAuthProvider(athleteId, AuthProvider.STRAVA)
                .orElseGet(() -> createStravaUser(athleteId, firstName, lastName, avatar, city, country));

        return respond(user);
    }

    private User createStravaUser(String athleteId, String firstName, String lastName,
                                  String avatar, String city, String country) {
        String placeholderEmail = "strava_" + athleteId + "@stride.local";
        // The unique constraint on email is the real guard — this up-front check just
        // yields a clearer error message (same as the monolith).
        if (userRepository.existsByEmail(placeholderEmail)) {
            throw new ApiRequestException(STRAVA_PLACEHOLDER_EMAIL_CONFLICT, HttpStatus.CONFLICT);
        }
        User user = userRepository.save(User.builder()
                .firstName(firstName != null ? firstName : "")
                .lastName(lastName != null ? lastName : "")
                .email(placeholderEmail)
                .authProvider(AuthProvider.STRAVA)
                .providerId(athleteId)
                .avatar(avatar)
                .city(city)
                .country(country)
                .role(Role.USER)
                .active(true)
                .build());
        events.publishUserRegistered(user);
        return user;
    }

    @Transactional(readOnly = true)
    public AuthenticationResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ApiRequestException(INVALID_JWT_TOKEN, HttpStatus.UNAUTHORIZED);
        }
        Claims claims;
        try {
            claims = tokens.parse(refreshToken);
        } catch (JwtException e) {
            throw new ApiRequestException(INVALID_JWT_TOKEN, HttpStatus.UNAUTHORIZED);
        }
        // Stricter than the monolith (which accepted any valid JWT here): only tokens
        // minted as refresh tokens are exchangeable.
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new ApiRequestException(INVALID_JWT_TOKEN, HttpStatus.UNAUTHORIZED);
        }
        Long userId = claims.get("userId", Number.class).longValue();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));
        return respond(user);
    }

    @Transactional
    public void forgotPassword(String email) {
        // Parity quirk preserved from the monolith: a 404 here reveals whether an email
        // has an account.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiRequestException(EMAIL_NOT_FOUND, HttpStatus.NOT_FOUND));

        String resetCode = UUID.randomUUID().toString();
        user.setPasswordResetCode(resetCode);
        user.setPasswordResetCodeExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        // TODO: Send email with reset code
        // emailService.sendPasswordResetEmail(user.getEmail(), resetCode);
    }

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

    private AuthenticationResponse respond(User user) {
        JwtTokenProvider.TokenPair pair = tokens.issueTokens(user.getId(), user.getEmail(), user.getRole());
        return new AuthenticationResponse(pair.accessToken(), pair.refreshToken(), UserResponse.from(user));
    }
}
