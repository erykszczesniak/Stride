package com.strava.userservice.auth;

import com.strava.userservice.exception.ApiRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StravaStateSigner")
class StravaStateSignerTest {

    private StravaStateSigner signer;

    @BeforeEach
    void setUp() {
        StravaProperties properties = new StravaProperties();
        properties.setStateSecret("state-secret-for-tests-at-least-32-chars");
        signer = new StravaStateSigner(properties);
    }

    @Test
    void signedLoginStateRoundTrips() {
        String state = signer.signLoginState();

        assertThatCode(() -> signer.verifyLoginState(state)).doesNotThrowAnyException();
    }

    @Test
    void fallsBackToJwtSecretWhenStateSecretBlank() {
        StravaProperties properties = new StravaProperties();
        properties.setStateSecret("");
        StravaStateSigner fallbackSigner = new StravaStateSigner(properties);
        fallbackSigner.setJwtSecretFallback("jwt-secret-fallback-at-least-32-chars!!");

        String state = fallbackSigner.signLoginState();

        assertThatCode(() -> fallbackSigner.verifyLoginState(state)).doesNotThrowAnyException();
    }

    @Test
    void tamperedSignatureIsRejected() {
        String state = signer.signLoginState();
        String tampered = state.substring(0, state.length() - 2) + "xx";

        assertThatThrownBy(() -> signer.verifyLoginState(tampered))
                .isInstanceOf(ApiRequestException.class)
                .hasMessageContaining("signature");
    }

    @Test
    void wrongMarkerIsRejected() {
        // Even a correctly signed payload must carry the login marker.
        long issuedAt = System.currentTimeMillis();
        String payload = "42." + issuedAt;
        String forged = payload + "." + signer.hmacBase64Url(payload);

        assertThatThrownBy(() -> signer.verifyLoginState(forged))
                .isInstanceOf(ApiRequestException.class)
                .hasMessageContaining("marker");
    }

    @Test
    void expiredStateIsRejected() {
        long issuedAt = System.currentTimeMillis() - StravaStateSigner.STATE_TTL_MILLIS - 1_000;
        String payload = StravaStateSigner.LOGIN_MARKER + "." + issuedAt;
        String expired = payload + "." + signer.hmacBase64Url(payload);

        assertThatThrownBy(() -> signer.verifyLoginState(expired))
                .isInstanceOf(ApiRequestException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void malformedStatesAreRejected() {
        assertThatThrownBy(() -> signer.verifyLoginState(null))
                .isInstanceOf(ApiRequestException.class);
        assertThatThrownBy(() -> signer.verifyLoginState(""))
                .isInstanceOf(ApiRequestException.class);
        assertThatThrownBy(() -> signer.verifyLoginState("only.two"))
                .isInstanceOf(ApiRequestException.class);
        assertThatThrownBy(() -> signer.verifyLoginState("login.notanumber.sig"))
                .isInstanceOf(ApiRequestException.class);
    }

    @Test
    void refusesWithoutAnySecret() {
        StravaProperties properties = new StravaProperties();
        properties.setStateSecret("");
        StravaStateSigner unconfigured = new StravaStateSigner(properties);
        unconfigured.setJwtSecretFallback("");

        assertThatThrownBy(unconfigured::signLoginState)
                .isInstanceOf(ApiRequestException.class)
                .hasMessageContaining("not configured")
                .extracting("status").isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThatThrownBy(() -> unconfigured.verifyLoginState("login.1.sig"))
                .isInstanceOf(ApiRequestException.class)
                .extracting("status").isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
