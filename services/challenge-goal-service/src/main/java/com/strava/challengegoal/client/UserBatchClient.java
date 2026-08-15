package com.strava.challengegoal.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Batch-resolves user display data (name + avatar) from user-service via
 * {@code lb://user-service/internal/users?ids=...}. Guarded by Resilience4j
 * (instance "user-service"); the fallback returns an empty map so read
 * endpoints stay up — callers render a "User #id" placeholder for ids the
 * lookup could not resolve.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserBatchClient {

    private final RestTemplate loadBalanced;

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallback")
    @Retry(name = "user-service")
    public Map<Long, UserSummary> usersByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();

        String joined = ids.stream()
                .distinct()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        var response = loadBalanced.exchange(
                "lb://user-service/internal/users?ids=" + joined,
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<UserSummary>>() {}
        );

        List<UserSummary> body = response.getBody();
        if (body == null) return Map.of();
        return body.stream()
                .filter(u -> u.id() != null)
                .collect(Collectors.toMap(UserSummary::id, Function.identity(), (a, b) -> a));
    }

    @SuppressWarnings("unused")
    private Map<Long, UserSummary> fallback(Collection<Long> ids, Throwable ex) {
        log.warn("UserBatchClient fallback for {} ids (cause: {})",
                ids == null ? 0 : ids.size(), ex.getMessage());
        return Map.of();
    }
}
