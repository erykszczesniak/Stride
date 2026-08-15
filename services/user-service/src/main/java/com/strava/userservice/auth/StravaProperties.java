package com.strava.userservice.auth;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Strava OAuth2 settings for the "Continue with Strava" login flow, populated from env
 * vars via {@code app.strava.*} — same keys as the monolith. Only the login-relevant
 * subset lives here; the import/integration flow (and its {@code redirect-uri}) belongs
 * to activity-service. Defaults are empty on purpose — the service boots without
 * credentials, but the login endpoints return 503.
 */
@Slf4j
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.strava")
public class StravaProperties {

    private String clientId = "";
    private String clientSecret = "";
    /** Callback Strava redirects the browser to; must be reachable through the gateway. */
    private String loginRedirectUri = "http://localhost:8080/api/v1/auth/strava/callback";
    /** Where the frontend picks up the tokens after a successful Strava login. */
    private String loginFrontendRedirect = "http://localhost:3000/auth/strava/callback";
    /** Where the frontend login page lives — used for error redirects on the login flow. */
    private String loginFrontendErrorRedirect = "http://localhost:3000/login";
    /**
     * Secret used to HMAC-sign the OAuth state parameter. Falls back to the JWT secret
     * when unset so dev boots don't require an extra env var.
     */
    private String stateSecret = "";

    /** Returns true only when both client id and secret are populated. */
    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }

    @PostConstruct
    void warnIfUnconfigured() {
        if (!isConfigured()) {
            log.warn("Strava login is not configured — set STRAVA_CLIENT_ID and "
                    + "STRAVA_CLIENT_SECRET to enable it. /api/v1/auth/strava/authorize "
                    + "will return 503.");
        } else {
            log.info("Strava login configured (clientId={}, loginRedirectUri={})",
                    clientId, loginRedirectUri);
        }
    }
}
