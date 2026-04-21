package com.strava.backend.controller;

import com.strava.backend.configuration.StravaProperties;
import com.strava.backend.dto.auth.AuthenticationResponse;
import com.strava.backend.service.AuthenticationService;
import com.strava.backend.service.impl.StravaStateSigner;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;

import static com.strava.backend.constants.PathConstants.API_V1_AUTH_STRAVA;
import static com.strava.backend.constants.PathConstants.AUTHORIZE;
import static com.strava.backend.constants.PathConstants.CALLBACK;

/**
 * "Continue with Strava" OAuth2 login/signup flow.
 * <p>
 * Deliberately separate from {@link StravaIntegrationController} — that controller's
 * endpoints require a logged-in user (they connect an existing account to a Strava
 * athlete), whereas this one's whole purpose is to let an unauthenticated user become
 * authenticated. Both flows share the Strava application (same client id/secret) and
 * the {@link StravaStateSigner} used to HMAC-sign the {@code state} query param.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(API_V1_AUTH_STRAVA)
public class StravaAuthController {

    static final String AUTH_URL = "https://www.strava.com/oauth/authorize";
    // Read scope is enough to get athlete id/name/avatar; profile:read_all gives us city/country too.
    static final String SCOPES = "read,profile:read_all";

    private final AuthenticationService authenticationService;
    private final StravaProperties stravaProperties;
    private final StravaStateSigner stateSigner;

    /**
     * Kick off the Strava login flow. Responds with either a JSON {@code {url: "..."}} so
     * the SPA can window.location itself, or a 302 when {@code ?redirect=true} — useful
     * for testing from a browser address bar.
     */
    @GetMapping(AUTHORIZE)
    public ResponseEntity<?> authorize(
            @RequestParam(value = "redirect", defaultValue = "false") boolean redirect) {
        if (!stravaProperties.isConfigured()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(java.util.Map.of("error",
                            "Strava login is not configured on this server"));
        }
        String state = stateSigner.signLoginState();
        String url = UriComponentsBuilder.fromHttpUrl(AUTH_URL)
                .queryParam("client_id", stravaProperties.getClientId())
                .queryParam("redirect_uri", stravaProperties.getLoginRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", SCOPES)
                .queryParam("state", state)
                .build()
                .toUriString();
        if (redirect) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, url)
                    .build();
        }
        return ResponseEntity.ok(java.util.Map.of("url", url));
    }

    /**
     * Strava redirects the browser here after the user approves (or denies) the app.
     * {@code permitAll} in the security config — identity arrives via the {@code code}
     * param, not our JWT.
     */
    @GetMapping(CALLBACK)
    public void callback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            HttpServletResponse response) throws IOException {

        String errorTarget = stravaProperties.getLoginFrontendErrorRedirect();
        if (errorTarget == null || errorTarget.isBlank()) {
            errorTarget = "/login";
        }

        if (error != null && !error.isBlank()) {
            log.info("Strava login callback received with error: {}", error);
            response.sendRedirect(withParam(errorTarget, "stravaError", "denied"));
            return;
        }

        try {
            stateSigner.verifyLoginState(state);
            AuthenticationResponse auth = authenticationService.stravaLogin(code);

            String successTarget = stravaProperties.getLoginFrontendRedirect();
            if (successTarget == null || successTarget.isBlank()) {
                successTarget = "/auth/strava/callback";
            }
            URI uri = UriComponentsBuilder.fromUriString(successTarget)
                    .queryParam("token", auth.getAccessToken())
                    .queryParam("refreshToken", auth.getRefreshToken())
                    .queryParam("userId", auth.getUser() != null ? auth.getUser().getId() : "")
                    .build()
                    .toUri();
            response.sendRedirect(uri.toString());
        } catch (Exception e) {
            log.warn("Strava login callback failed: {}", e.getMessage());
            response.sendRedirect(withParam(errorTarget, "stravaError", "callback-failed"));
        }
    }

    private static String withParam(String baseUrl, String key, String value) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam(key, value)
                .build()
                .toUri()
                .toString();
    }
}
