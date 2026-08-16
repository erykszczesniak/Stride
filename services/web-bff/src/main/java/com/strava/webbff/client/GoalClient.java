package com.strava.webbff.client;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Calls challenge-goal-service via Spring Cloud LoadBalancer
 * (lb://challenge-goal-service), identity forwarded on X-User-* headers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoalClient {

    private static final String BASE = "lb://challenge-goal-service";

    private static final ParameterizedTypeReference<List<JsonNode>> JSON_LIST =
            new ParameterizedTypeReference<>() {
            };

    private final RestTemplate loadBalanced;

    /**
     * The caller's active goals (all periods, computed fields included) —
     * the dashboard picks the WEEKLY one, passed through verbatim.
     */
    @CircuitBreaker(name = "challenge-goal-service", fallbackMethod = "listFallback")
    @Retry(name = "challenge-goal-service")
    public List<JsonNode> activeGoals() {
        return list(BASE + "/api/v1/goals");
    }

    /** Active challenges (first {@code size}) — used only to approximate the admin total. */
    @CircuitBreaker(name = "challenge-goal-service", fallbackMethod = "sizedListFallback")
    @Retry(name = "challenge-goal-service")
    public List<JsonNode> activeChallenges(int size) {
        return list(BASE + "/api/v1/challenges?page=0&size=" + size);
    }

    private List<JsonNode> list(String url) {
        ResponseEntity<List<JsonNode>> response =
                loadBalanced.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, JSON_LIST);
        return response.getBody() != null ? response.getBody() : List.of();
    }

    /** Fallback: the dashboard shows no weekly goal instead of failing. */
    @SuppressWarnings("unused")
    private List<JsonNode> listFallback(Throwable ex) {
        log.warn("challenge-goal-service unavailable, goals degrade to [] (cause: {})", ex.getMessage());
        return List.of();
    }

    /** Fallback: admin challenge count degrades to 0. */
    @SuppressWarnings("unused")
    private List<JsonNode> sizedListFallback(int size, Throwable ex) {
        log.warn("challenge-goal-service unavailable, challenge count degrades to 0 (cause: {})", ex.getMessage());
        return List.of();
    }
}
