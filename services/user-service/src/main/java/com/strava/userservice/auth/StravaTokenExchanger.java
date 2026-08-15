package com.strava.userservice.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.userservice.exception.ApiRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Thin wrapper around Strava's {@code POST /oauth/token} endpoint, ported from the
 * monolith so the login flow can exchange authorization codes for athlete data.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StravaTokenExchanger {

    static final String TOKEN_URL = "https://www.strava.com/api/v3/oauth/token";

    private final StravaProperties stravaProperties;
    private final ObjectMapper objectMapper;

    /** Instantiated directly (not a Spring bean) so tests can swap via {@link #setRestTemplate}. */
    private RestTemplate restTemplate = new RestTemplate();

    /**
     * Exchange an authorization {@code code} for tokens. The {@code redirectUri} parameter
     * must match the one used on the authorize call.
     *
     * @return the raw token response JSON — the caller decides what to pull out of it
     */
    public JsonNode exchange(String code, String redirectUri) {
        requireConfigured();
        if (code == null || code.isBlank()) {
            throw new ApiRequestException("Missing Strava authorization code", HttpStatus.BAD_REQUEST);
        }

        String body = "client_id=" + urlEncode(stravaProperties.getClientId())
                + "&client_secret=" + urlEncode(stravaProperties.getClientSecret())
                + "&code=" + urlEncode(code)
                + "&grant_type=authorization_code";
        if (redirectUri != null && !redirectUri.isBlank()) {
            body += "&redirect_uri=" + urlEncode(redirectUri);
        }
        return postForm(body, "exchange code");
    }

    private JsonNode postForm(String formBody, String op) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<String> entity = new HttpEntity<>(formBody, headers);
        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(TOKEN_URL, HttpMethod.POST, entity, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (HttpClientErrorException e) {
            log.warn("Strava {} failed: {} {}", op, e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiRequestException(
                    "Strava " + op + " failed: " + e.getStatusCode(), HttpStatus.BAD_GATEWAY);
        } catch (Exception e) {
            log.error("Strava {} failed", op, e);
            throw new ApiRequestException("Strava " + op + " failed", HttpStatus.BAD_GATEWAY);
        }
    }

    private void requireConfigured() {
        if (!stravaProperties.isConfigured()) {
            throw new ApiRequestException(
                    "Strava integration is not configured on this server",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    // Test seam
    void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
}
