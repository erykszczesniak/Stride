package com.strava.userservice.auth;

import com.strava.userservice.exception.ApiRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static com.strava.userservice.common.ErrorMessages.INVALID_GOOGLE_TOKEN;

/**
 * Verifies a Google ID token against Google's tokeninfo endpoint, exactly like the
 * monolith's {@code AuthenticationServiceImpl.googleLogin} (which inlined the call).
 * Extracted so the HTTP hop is mockable in unit tests.
 */
@Slf4j
@Component
public class GoogleTokenVerifier {

    static final String TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";

    /** Instantiated directly (not a Spring bean) so tests can swap via {@link #setRestTemplate}. */
    private RestTemplate restTemplate = new RestTemplate();

    /**
     * @return the raw tokeninfo claims (email, given_name, family_name, sub, picture, ...)
     * @throws ApiRequestException 401 when the token is rejected or the call fails
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> verify(String idToken) {
        Map<String, Object> tokenInfo;
        try {
            tokenInfo = restTemplate.getForObject(TOKEN_INFO_URL + idToken, Map.class);
        } catch (Exception e) {
            log.error("Failed to verify Google ID token", e);
            throw new ApiRequestException(INVALID_GOOGLE_TOKEN, HttpStatus.UNAUTHORIZED);
        }
        if (tokenInfo == null || tokenInfo.containsKey("error_description")) {
            throw new ApiRequestException(INVALID_GOOGLE_TOKEN, HttpStatus.UNAUTHORIZED);
        }
        return tokenInfo;
    }

    // Test seam
    void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
}
