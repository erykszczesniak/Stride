package com.strava.socialservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Batch activity hydration from activity-service's cluster-internal API —
 * one call per feed page instead of N+1 per row. Resolved via Spring Cloud
 * LoadBalancer (lb://activity-service), wrapped in Resilience4j retry +
 * circuit breaker (instance "activity-service") — the same protection stack
 * challenge-goal-service uses for its activity reads.
 *
 * The return value distinguishes two very different situations:
 *   * {@code Optional.of(map)} — the call succeeded; an id absent from the
 *     map means activity-service no longer has that activity (deleted but
 *     the event not yet consumed) and the caller should DROP the row, exactly
 *     like main drops feed entries whose Activity lookup returns nothing;
 *   * {@code Optional.empty()} — activity-service is down / circuit open;
 *     the caller degrades to the local feed_items snapshot instead.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityBatchClient {

    private final RestTemplate loadBalanced;

    @CircuitBreaker(name = "activity-service", fallbackMethod = "fallback")
    @Retry(name = "activity-service")
    public Optional<Map<Long, InternalActivityDto>> activitiesById(Collection<Long> activityIds) {
        if (activityIds == null || activityIds.isEmpty()) {
            return Optional.of(Map.of());
        }
        String ids = activityIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String url = "lb://activity-service/internal/activities?ids=" + ids;
        var response = loadBalanced.exchange(url, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<InternalActivityDto>>() {});
        List<InternalActivityDto> body = response.getBody() != null ? response.getBody() : List.of();
        return Optional.of(body.stream()
                .filter(a -> a.id() != null)
                .collect(Collectors.toMap(InternalActivityDto::id, Function.identity(), (a, b) -> a)));
    }

    @SuppressWarnings("unused")
    private Optional<Map<Long, InternalActivityDto>> fallback(Collection<Long> activityIds, Throwable ex) {
        log.warn("ActivityBatchClient fallback for {} ids (cause: {})",
                activityIds == null ? 0 : activityIds.size(), ex.getMessage());
        return Optional.empty();
    }
}
