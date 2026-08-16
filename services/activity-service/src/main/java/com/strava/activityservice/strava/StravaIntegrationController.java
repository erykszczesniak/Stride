package com.strava.activityservice.strava;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

/**
 * OAuth2 + import endpoints for Strava integration, mirroring main's
 * StravaIntegrationController.
 * <p>
 * Flow:
 * <ol>
 *   <li>UI calls {@code GET /authorize} (authenticated) → we return / 302 to
 *       strava.com/oauth/authorize with an HMAC-signed {@code state} containing the user id.</li>
 *   <li>Strava redirects the browser to {@code GET /callback?code=...&state=...} — this
 *       endpoint is reachable WITHOUT the X-User identity headers (the gateway allowlists
 *       it and this service's SecurityConfig permits it) and recovers the user id from
 *       the state's HMAC, then swaps the code for tokens.</li>
 *   <li>After a successful connect we 302 to the frontend settings page with a flag.</li>
 * </ol>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/integrations/strava")
public class StravaIntegrationController {

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
    @GetMapping("/authorize")
    public ResponseEntity<?> authorize(
            @AuthenticationPrincipal Long currentUserId,
            @RequestParam(value = "redirect", defaultValue = "false") boolean redirect) {
        String url = stravaService.buildAuthorizeUrl(currentUserId);
        if (redirect) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, url)
                    .build();
        }
        return ResponseEntity.ok(Map.of("url", url));
    }

    /**
     * Open to any caller — Strava itself redirects the browser here without our JWT
     * or the gateway's identity headers. Identity is recovered from the signed
     * {@code state} parameter.
     */
    @GetMapping("/callback")
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

    @PostMapping("/import")
    public ResponseEntity<StravaImportResponse> startImport(@AuthenticationPrincipal Long currentUserId) {
        stravaService.startImportForUser(currentUserId);
        return ResponseEntity.accepted().body(StravaImportResponse.builder()
                .message("Strava import queued — check back shortly.")
                .started(true)
                .build());
    }

    @GetMapping("/status")
    public ResponseEntity<StravaStatusResponse> getStatus(@AuthenticationPrincipal Long currentUserId) {
        return ResponseEntity.ok(stravaService.getStatus(currentUserId));
    }

    @DeleteMapping("/disconnect")
    public ResponseEntity<Void> disconnect(@AuthenticationPrincipal Long currentUserId) {
        stravaService.disconnect(currentUserId);
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
