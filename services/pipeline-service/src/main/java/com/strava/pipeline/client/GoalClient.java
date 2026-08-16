package com.strava.pipeline.client;

import com.strava.shared.security.UserContext;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Active goals from challenge-goal-service (GET /api/v1/goals returns active
 * goals only — the same set main's goalService.getActiveGoalsByUserId gave the
 * pipeline). Used solely for the season-goal target time, which main derives
 * from the first active TIME goal; on failure the pipeline degrades to
 * targetSeconds=0 exactly like a user without goals.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoalClient {

    private final RestTemplate loadBalanced;

    public record GoalData(String goalType, Double targetValue) {
    }

    @CircuitBreaker(name = "challenge-goal-service", fallbackMethod = "goalsFallback")
    @Retry(name = "challenge-goal-service")
    public List<GoalData> activeGoals(Long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(UserContext.HEADER_USER_ID, userId.toString());
        var response = loadBalanced.exchange("lb://challenge-goal-service/api/v1/goals",
                HttpMethod.GET, new HttpEntity<>(headers),
                new ParameterizedTypeReference<List<GoalData>>() {
                });
        List<GoalData> body = response.getBody();
        return body == null ? List.of() : body;
    }

    @SuppressWarnings("unused")
    private List<GoalData> goalsFallback(Long userId, Throwable ex) {
        log.warn("GoalClient fallback for userId={} (cause: {})", userId, ex.getMessage());
        return List.of();
    }
}
