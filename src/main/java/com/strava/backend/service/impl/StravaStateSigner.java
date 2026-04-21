package com.strava.backend.service.impl;

import com.strava.backend.configuration.StravaProperties;
import com.strava.backend.exception.ApiRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * HMAC-SHA256 signer for the {@code state} parameter in the OAuth redirect.
 * <p>
 * Format: {@code <userId>.<issuedAtMillis>.<base64UrlSignature>}. The signature covers
 * {@code userId.issuedAt}. We also enforce a 10-minute TTL on the issuedAt timestamp so
 * a stolen/leaked state can't be replayed indefinitely.
 */
@Component
@RequiredArgsConstructor
public class StravaStateSigner {

    static final long STATE_TTL_MILLIS = 10 * 60 * 1000L; // 10 minutes

    private final StravaProperties stravaProperties;

    /** Fall back to the JWT secret when {@code app.strava.state-secret} is blank. */
    @Value("${jwt.secret:changeme}")
    private String jwtSecretFallback;

    /** Marker payload used on the unauthenticated login/signup flow where no userId exists yet. */
    static final String LOGIN_MARKER = "login";

    public String sign(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required to sign state");
        }
        long issuedAt = System.currentTimeMillis();
        String payload = userId + "." + issuedAt;
        String sig = hmacBase64Url(payload);
        return payload + "." + sig;
    }

    /** Returns the userId the state was issued for, or throws if signature/TTL is bad. */
    public Long verify(String state) {
        String[] parts = parseAndCheck(state);
        long userId;
        long issuedAt;
        try {
            userId = Long.parseLong(parts[0]);
            issuedAt = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            throw new ApiRequestException("Malformed Strava OAuth state", HttpStatus.BAD_REQUEST);
        }

        String expectedSig = hmacBase64Url(userId + "." + issuedAt);
        if (!constantTimeEquals(expectedSig, parts[2])) {
            throw new ApiRequestException("Invalid Strava OAuth state signature", HttpStatus.BAD_REQUEST);
        }
        if (System.currentTimeMillis() - issuedAt > STATE_TTL_MILLIS) {
            throw new ApiRequestException("Strava OAuth state has expired", HttpStatus.BAD_REQUEST);
        }
        return userId;
    }

    /**
     * Sign a state parameter for the no-user login flow. Uses a fixed marker payload because
     * the HMAC signature (keyed on a server secret) is what prevents forgery — an attacker
     * can't construct a valid state without the secret.
     */
    public String signLoginState() {
        long issuedAt = System.currentTimeMillis();
        String payload = LOGIN_MARKER + "." + issuedAt;
        String sig = hmacBase64Url(payload);
        return payload + "." + sig;
    }

    /** Throws if the state isn't a valid login-flow state (bad signature or expired). */
    public void verifyLoginState(String state) {
        String[] parts = parseAndCheck(state);
        if (!LOGIN_MARKER.equals(parts[0])) {
            throw new ApiRequestException("Unexpected Strava OAuth state marker", HttpStatus.BAD_REQUEST);
        }
        long issuedAt;
        try {
            issuedAt = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            throw new ApiRequestException("Malformed Strava OAuth state", HttpStatus.BAD_REQUEST);
        }
        String expectedSig = hmacBase64Url(LOGIN_MARKER + "." + issuedAt);
        if (!constantTimeEquals(expectedSig, parts[2])) {
            throw new ApiRequestException("Invalid Strava OAuth state signature", HttpStatus.BAD_REQUEST);
        }
        if (System.currentTimeMillis() - issuedAt > STATE_TTL_MILLIS) {
            throw new ApiRequestException("Strava OAuth state has expired", HttpStatus.BAD_REQUEST);
        }
    }

    private static String[] parseAndCheck(String state) {
        if (state == null || state.isBlank()) {
            throw new ApiRequestException("Invalid Strava OAuth state", HttpStatus.BAD_REQUEST);
        }
        String[] parts = state.split("\\.");
        if (parts.length != 3) {
            throw new ApiRequestException("Malformed Strava OAuth state", HttpStatus.BAD_REQUEST);
        }
        return parts;
    }

    private String hmacBase64Url(String payload) {
        String secret = stravaProperties.getStateSecret();
        if (secret == null || secret.isBlank()) {
            secret = jwtSecretFallback;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute Strava state signature", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
