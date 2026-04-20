package com.strava.backend.controller;

import com.strava.backend.configuration.StravaProperties;
import com.strava.backend.dto.strava.StravaImportResponse;
import com.strava.backend.dto.strava.StravaStatusResponse;
import com.strava.backend.service.StravaIntegrationService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;

import static com.strava.backend.constants.PathConstants.*;

/**
 * OAuth2 + import endpoints for Strava integration.
 * <p>
 * Flow:
 * <ol>
 *   <li>UI calls {@code GET /authorize} (authenticated) → we 302 to strava.com/oauth/authorize
 *       with an HMAC-signed {@code state} containing the user id.</li>
 *   <li>Strava redirects back to {@code GET /callback?code=...&state=...} — this endpoint is
 *       {@code permitAll} (Strava calls it without our JWT) and recovers the user id from
 *       the state's HMAC, then swaps the code for tokens.</li>
 *   <li>After a successful connect we 302 to the frontend settings page with a flag.</li>
 * </ol>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(API_V1_INTEGRATIONS_STRAVA)
public class StravaIntegrationController extends BaseController {

    private final StravaIntegrationService stravaService;
    private final StravaProperties stravaProperties;

    /**
     * Returns the Strava authorization URL the client should navigate to. Two response
     * modes for caller convenience:
     * <ul>
     *     <li>Default: JSON {@code {url: "..."}} so the SPA can window.location itself.</li>
     *     <li>{@code ?redirect=true}: 302 straight to strava.com — handy for testing from
     *         a browser address bar.</li>
     * </ul>
     */
    @GetMapping(AUTHORIZE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> authorize(
            @RequestParam(value = "redirect", defaultValue = "false") boolean redirect) {
        Long userId = getAuthenticatedUserId();
        String url = stravaService.buildAuthorizeUrl(userId);
        if (redirect) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, url)
                    .build();
        }
        return ResponseEntity.ok(java.util.Map.of("url", url));
    }

    /**
     * Open to any caller — Strava itself redirects the browser here without our JWT.
     * Identity is recovered from the signed {@code state} parameter.
     */
    @GetMapping(CALLBACK)
    public void callback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            HttpServletResponse response) throws IOException {

        String target = stravaProperties.getFrontendRedirect();
        if (target == null || target.isBlank()) {
            target = "/settings?section=account";
        }

        if (error != null && !error.isBlank()) {
            log.warn("Strava callback received with error: {}", error);
            response.sendRedirect(withParam(target, "stravaError", error));
            return;
        }

        try {
            Long userId = stravaService.verifyStateAndExtractUserId(state);
            stravaService.exchangeCodeAndStoreConnection(userId, code);
            response.sendRedirect(withParam(target, "stravaConnected", "true"));
        } catch (Exception e) {
            log.warn("Strava callback failed: {}", e.getMessage());
            response.sendRedirect(withParam(target, "stravaError", "connection_failed"));
        }
    }

    @PostMapping(IMPORT)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StravaImportResponse> startImport() {
        Long userId = getAuthenticatedUserId();
        stravaService.startImportForUser(userId);
        return ResponseEntity.accepted().body(StravaImportResponse.builder()
                .message("Strava import queued — check back shortly.")
                .started(true)
                .build());
    }

    @GetMapping(STATUS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StravaStatusResponse> getStatus() {
        Long userId = getAuthenticatedUserId();
        return ResponseEntity.ok(stravaService.getStatus(userId));
    }

    @DeleteMapping(DISCONNECT)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> disconnect() {
        Long userId = getAuthenticatedUserId();
        stravaService.disconnect(userId);
        return ResponseEntity.noContent().build();
    }

    private static String withParam(String baseUrl, String key, String value) {
        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam(key, value)
                .build()
                .toUri();
        return uri.toString();
    }
}
