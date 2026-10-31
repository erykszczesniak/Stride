package com.strava.activityservice.strava;

import com.strava.activityservice.exception.ApiRequestException;
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
    @DisplayName("sign requires a user id")
    void signRequiresUserId() {
        assertThatThrownBy(() -> signer.sign(null))
                .isInstanceOf(IllegalArgumentException.class);
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
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("a state signed with a different secret is rejected")
    void foreignSecretRejected() {
        StravaProperties other = new StravaProperties();
        other.setStateSecret("some-other-secret");
        StravaStateSigner foreignSigner = new StravaStateSigner(other);

        String state = foreignSigner.sign(42L);

        assertThatThrownBy(() -> signer.verify(state))
                .isInstanceOf(ApiRequestException.class)
                .hasMessageContaining("signature");
    }

    @Test
    @DisplayName("an expired state (older than the 10-minute TTL) is rejected")
    void expiredStateRejected() {
        long staleIssuedAt = System.currentTimeMillis() - StravaStateSigner.STATE_TTL_MILLIS - 1_000;
        // Re-sign a stale payload with the real secret so only the TTL check trips.
        String payload = 42L + "." + staleIssuedAt;
        String sig = (String) ReflectionTestUtils.invokeMethod(signer, "hmacBase64Url", payload);
        String state = payload + "." + sig;

        assertThatThrownBy(() -> signer.verify(state))
                .isInstanceOf(ApiRequestException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("malformed states are rejected with 400")
    void malformedStatesRejected() {
        assertThatThrownBy(() -> signer.verify(null)).isInstanceOf(ApiRequestException.class);
        assertThatThrownBy(() -> signer.verify("")).isInstanceOf(ApiRequestException.class);
        assertThatThrownBy(() -> signer.verify("only.two")).isInstanceOf(ApiRequestException.class);
        assertThatThrownBy(() -> signer.verify("not-a-number.123.sig"))
                .isInstanceOf(ApiRequestException.class)
                .hasMessageContaining("Malformed");
    }

    @Test
    @DisplayName("falls back to the jwt.secret value when state-secret is blank")
    void fallsBackToJwtSecret() {
        StravaProperties blank = new StravaProperties();
        blank.setStateSecret("");
        StravaStateSigner fallbackSigner = new StravaStateSigner(blank);
        ReflectionTestUtils.setField(fallbackSigner, "jwtSecretFallback", "jwt-fallback-secret");

        String state = fallbackSigner.sign(7L);

        assertThat(fallbackSigner.verify(state)).isEqualTo(7L);
    }

    @Test
    @DisplayName("refuses to sign or verify when neither secret is configured")
    void refusesWithoutAnySecret() {
        StravaProperties blank = new StravaProperties();
        blank.setStateSecret("");
        StravaStateSigner unconfigured = new StravaStateSigner(blank);
        ReflectionTestUtils.setField(unconfigured, "jwtSecretFallback", "");

        assertThatThrownBy(() -> unconfigured.sign(7L))
                .isInstanceOf(ApiRequestException.class)
                .hasMessageContaining("not configured")
                .extracting("status").isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThatThrownBy(() -> unconfigured.verify("7.1.sig"))
                .isInstanceOf(ApiRequestException.class)
                .extracting("status").isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
