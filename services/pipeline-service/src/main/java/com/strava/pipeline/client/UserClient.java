package com.strava.pipeline.client;

import com.strava.pipeline.exception.ApiRequestException;
import com.strava.shared.security.UserContext;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Athlete identity fields for the pipeline profile, from user-service's
 * GET /api/v1/users/me. A downstream 404 keeps main's "User not found"
 * semantics; infrastructure failures surface as the contract's server_error.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserClient {

    private final RestTemplate loadBalanced;

    public record UserData(String firstName, String lastName, String city) {
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "meFallback")
    @Retry(name = "user-service")
    public UserData me(Long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(UserContext.HEADER_USER_ID, userId.toString());
        try {
            var response = loadBalanced.exchange("lb://user-service/api/v1/users/me",
                    HttpMethod.GET, new HttpEntity<>(headers), UserData.class);
            UserData body = response.getBody();
            if (body == null) {
                throw new ApiRequestException("User not found", HttpStatus.NOT_FOUND);
            }
            return body;
        } catch (HttpClientErrorException.NotFound e) {
            throw new ApiRequestException("User not found", HttpStatus.NOT_FOUND);
        }
    }

    @SuppressWarnings("unused")
    private UserData meFallback(Long userId, Throwable ex) {
        if (ex instanceof ApiRequestException api) {
            throw api;
        }
        log.warn("UserClient failure for userId={} (cause: {})", userId, ex.getMessage());
        throw new ApiRequestException("User profile unavailable", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
