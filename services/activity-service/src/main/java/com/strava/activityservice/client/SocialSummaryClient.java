package com.strava.activityservice.client;

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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Batch social counts (likes/comments/liked-by-viewer) from social-service's
 * cluster-internal API, one call per response page instead of N+1 per row.
 * Resolved via Spring Cloud LoadBalancer (lb://social-service), wrapped in
 * Resilience4j retry + circuit breaker (instance "social-service").
 *
 * The fallback returns no summaries, which the mappers render as
 * zeros/likedByCurrentUser=false — activity endpoints stay up when
 * social-service is down (or, during the migration phases, not yet deployed).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SocialSummaryClient {

    private final RestTemplate loadBalanced;

    public record SocialSummary(Long activityId, int likesCount, int commentsCount, boolean likedByViewer) {

        public static SocialSummary empty(Long activityId) {
            return new SocialSummary(activityId, 0, 0, false);
        }
    }

    /** Summaries keyed by activity id; ids missing from the response fall back to zeros. */
    @CircuitBreaker(name = "social-service", fallbackMethod = "fallback")
    @Retry(name = "social-service")
    public Map<Long, SocialSummary> summariesFor(Collection<Long> activityIds, Long viewerId) {
        if (activityIds == null || activityIds.isEmpty()) {
            return Map.of();
        }
        String ids = activityIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String url = "lb://social-service/internal/social/activities?ids=" + ids
                + (viewerId != null ? "&viewerId=" + viewerId : "");
        var response = loadBalanced.exchange(url, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<SocialSummary>>() {});
        List<SocialSummary> body = response.getBody() != null ? response.getBody() : List.of();
        return body.stream()
                .filter(s -> s.activityId() != null)
                .collect(Collectors.toMap(SocialSummary::activityId, Function.identity(), (a, b) -> a));
    }

    @SuppressWarnings("unused")
    private Map<Long, SocialSummary> fallback(Collection<Long> activityIds, Long viewerId, Throwable ex) {
        log.warn("SocialSummaryClient fallback for {} ids (cause: {})",
                activityIds == null ? 0 : activityIds.size(), ex.getMessage());
        return Map.of();
    }
}
