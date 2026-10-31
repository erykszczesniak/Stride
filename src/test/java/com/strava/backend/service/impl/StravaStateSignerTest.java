package com.strava.backend.service.impl;

import com.strava.backend.configuration.StravaProperties;
import com.strava.backend.exception.ApiRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StravaStateSigner Unit Tests")
class StravaStateSignerTest {

    private StravaProperties properties;
    private StravaStateSigner signer;

    @BeforeEach
    void setUp() {
        properties = new StravaProperties();
        properties.setStateSecret("test-state-secret");
        signer = new StravaStateSigner(properties);
    }

    @Test
    @DisplayName("sign/verify round-trips the user id")
    void roundTrip() {
        String state = signer.sign(42L);

        assertThat(state.split("\\.")).hasSize(3);
        assertThat(signer.verify(state)).isEqualTo(42L);
    }

    @Test
    @DisplayName("login state round-trips without a user id")
    void loginStateRoundTrip() {
        String state = signer.signLoginState();

        assertThat(state).startsWith("login.");
        signer.verifyLoginState(state);
    }

    @Test
    @DisplayName("a tampered payload fails signature verification")
    void tamperedPayloadRejected() {
        String state = signer.sign(42L);
        String[] parts = state.split("\\.");
        String tampered = "43." + parts[1] + "." + parts[2];

        assertThatThrownBy(() -> signer.verify(tampered))
                .isInstanceOf(ApiRequestException.class)
                .hasMessageContaining("signature")
                .extracting(e -> ((ApiRequestException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("falls back to jwt.secret when the state secret is blank")
    void fallsBackToJwtSecret() {
        StravaProperties blank = new StravaProperties();
        blank.setStateSecret("");
        StravaStateSigner fallbackSigner = new StravaStateSigner(blank);
        ReflectionTestUtils.setField(fallbackSigner, "jwtSecretFallback", "jwt-secret-fallback-at-least-32-chars!!");

        String state = fallbackSigner.sign(7L);

        assertThat(fallbackSigner.verify(state)).isEqualTo(7L);
        assertThatThrownBy(() -> signer.verify(state))
                .isInstanceOf(ApiRequestException.class)
                .hasMessageContaining("signature");
    }

    @Test
    @DisplayName("refuses to sign when neither secret is configured")
    void refusesWithoutAnySecret() {
        StravaProperties blank = new StravaProperties();
        blank.setStateSecret("");
        StravaStateSigner unconfigured = new StravaStateSigner(blank);
        ReflectionTestUtils.setField(unconfigured, "jwtSecretFallback", "");

        assertThatThrownBy(() -> unconfigured.sign(7L))
                .isInstanceOf(ApiRequestException.class)
                .hasMessageContaining("not configured")
                .extracting(e -> ((ApiRequestException) e).getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThatThrownBy(() -> unconfigured.verifyLoginState("login.1.sig"))
                .isInstanceOf(ApiRequestException.class)
                .extracting(e -> ((ApiRequestException) e).getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
