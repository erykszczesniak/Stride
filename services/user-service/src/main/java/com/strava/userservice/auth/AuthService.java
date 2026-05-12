package com.strava.userservice.auth;

import com.strava.userservice.domain.AuthProvider;
import com.strava.userservice.domain.Role;
import com.strava.userservice.domain.User;
import com.strava.userservice.domain.UserRepository;
import com.strava.userservice.event.UserEventPublisher;
import com.strava.userservice.exception.AuthException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokens;
    private final UserEventPublisher events;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (users.existsByEmail(req.email())) {
            throw new AuthException("Email already registered");
        }
        User user = User.builder()
                .firstName(req.firstName())
                .lastName(req.lastName())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .authProvider(AuthProvider.LOCAL)
                .role(Role.USER)
                .build();
        users.save(user);
        events.publishUserRegistered(user);
        return tokenize(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmail(req.email())
                .orElseThrow(() -> new AuthException("Invalid credentials"));
        if (user.getPassword() == null || !passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new AuthException("Invalid credentials");
        }
        if (!user.isActive()) {
            throw new AuthException("Account is inactive");
        }
        return tokenize(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest req) {
        Claims claims;
        try {
            claims = tokens.parse(req.refreshToken());
        } catch (JwtException e) {
            throw new AuthException("Invalid refresh token");
        }
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new AuthException("Not a refresh token");
        }
        Long userId = claims.get("userId", Number.class).longValue();
        User user = users.findById(userId)
                .orElseThrow(() -> new AuthException("User no longer exists"));
        return tokenize(user);
    }

    private AuthResponse tokenize(User user) {
        JwtTokenProvider.TokenPair pair = tokens.issueTokens(user.getId(), user.getEmail(), user.getRole());
        return new AuthResponse(
                pair.accessToken(),
                pair.refreshToken(),
                pair.expiresInSeconds(),
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name()
        );
    }
}
