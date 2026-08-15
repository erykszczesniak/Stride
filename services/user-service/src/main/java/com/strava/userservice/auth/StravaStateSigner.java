package com.strava.userservice.auth;

import com.strava.userservice.exception.ApiRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * HMAC-SHA256 signer for the {@code state} parameter of the Strava OAuth login redirect,
 * ported from the monolith's {@code StravaStateSigner} (login-flow subset — the
 * per-user integration flow lives in activity-service).
 * <p>
 * Format: {@code login.<issuedAtMillis>.<base64UrlSignature>}. The signature covers
 * {@code login.issuedAt}; a 10-minute TTL prevents indefinite replay of a leaked state.
 */
@Component
@RequiredArgsConstructor
public class StravaStateSigner {

    static final long STATE_TTL_MILLIS = 10 * 60 * 1000L; // 10 minutes

    /** Marker payload used on the unauthenticated login/signup flow where no userId exists yet. */
    static final String LOGIN_MARKER = "login";

    private final StravaProperties stravaProperties;

    /** Fall back to the JWT secret when {@code app.strava.state-secret} is blank. */
    @Value("${jwt.secret:changeme}")
    private String jwtSecretFallback;

    /**
     * Sign a state parameter for the no-user login flow. Uses a fixed marker payload because
     * the HMAC signature (keyed on a server secret) is what prevents forgery — an attacker
     * can't construct a valid state without the secret.
     */
    public String signLoginState() {
        long issuedAt = System.currentTimeMillis();
        String payload = LOGIN_MARKER + "." + issuedAt;
        return payload + "." + hmacBase64Url(payload);
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

    String hmacBase64Url(String payload) {
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

    // Test seam — lets unit tests craft states with a chosen issuedAt.
    void setJwtSecretFallback(String jwtSecretFallback) {
        this.jwtSecretFallback = jwtSecretFallback;
    }
}
