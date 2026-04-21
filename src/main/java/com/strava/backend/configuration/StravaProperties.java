package com.strava.backend.configuration;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Strava OAuth2 + API integration settings. Populated from env vars via {@code app.strava.*}
 * in application.yml. Defaults are empty on purpose — the app still boots without credentials,
 * but the integration endpoints will refuse to run and a WARN is logged at startup.
 */
@Slf4j
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.strava")
public class StravaProperties {

    private String clientId = "";
    private String clientSecret = "";
    private String redirectUri = "";
    /**
     * Callback for the "Continue with Strava" login/signup flow. Separate from
     * {@link #redirectUri} which is used by the integration/import flow. Strava allows
     * any path under the registered callback domain, so both can coexist.
     */
    private String loginRedirectUri = "http://localhost:8080/api/v1/auth/strava/callback";
    /** Where the frontend picks up the tokens after a successful Strava login. */
    private String loginFrontendRedirect = "http://localhost:3000/auth/strava/callback";
    /**
     * Secret used to HMAC-sign the OAuth state parameter. Falls back to the JWT secret
     * when unset so dev boots don't require an extra env var.
     */
    private String stateSecret = "";
    /** Where we bounce the user after a successful callback. */
    private String frontendRedirect = "http://localhost:3000/settings?section=account";
    /** Where the frontend login page lives — used for error redirects on the login flow. */
    private String loginFrontendErrorRedirect = "http://localhost:3000/login";

    /** Returns true only when both client id and secret are populated. */
    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }

    @PostConstruct
    void warnIfUnconfigured() {
        if (!isConfigured()) {
            log.warn("Strava integration is not configured — set STRAVA_CLIENT_ID, "
                    + "STRAVA_CLIENT_SECRET, STRAVA_REDIRECT_URI to enable it. "
                    + "Endpoints under /api/v1/integrations/strava will return 503.");
        } else {
            log.info("Strava integration configured (clientId={}, redirectUri={})",
                    clientId, redirectUri);
        }
    }
}
