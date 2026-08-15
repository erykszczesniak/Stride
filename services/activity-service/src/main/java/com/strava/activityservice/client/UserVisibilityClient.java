package com.strava.activityservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Owner-profile privacy + follow facts from user-service's cluster-internal
 * API, resolved via Spring Cloud LoadBalancer (lb://user-service) through
 * Eureka. Wrapped in Resilience4j retry + circuit breaker (instance
 * "user-service").
 *
 * Fallback tradeoff: on failure we report {isPrivate:false, viewerFollows:false},
 * i.e. we treat the owner's PROFILE as not private. Failing closed is
 * impossible without the data — it would blank every profile page and feed
 * card whenever user-service blips, hiding the 99% public content to protect
 * the rare private profile. Activity-level isPrivate is stored locally and
 * stays enforced regardless, so a private ACTIVITY never leaks; only the
 * profile-wide privacy screen degrades during an outage.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserVisibilityClient {

    private final RestTemplate loadBalanced;

    public record VisibilityInfo(boolean isPrivate, boolean viewerFollows) {

        public static final VisibilityInfo FALLBACK = new VisibilityInfo(false, false);
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallback")
    @Retry(name = "user-service")
    public VisibilityInfo visibilityFor(Long ownerId, Long viewerId) {
        String url = "lb://user-service/internal/users/" + ownerId + "/visibility"
                + (viewerId != null ? "?viewerId=" + viewerId : "");
        VisibilityInfo info = loadBalanced.getForObject(url, VisibilityInfo.class);
        return info != null ? info : VisibilityInfo.FALLBACK;
    }

    @SuppressWarnings("unused")
    private VisibilityInfo fallback(Long ownerId, Long viewerId, Throwable ex) {
        log.warn("UserVisibilityClient fallback for ownerId={} viewerId={} (cause: {})",
                ownerId, viewerId, ex.getMessage());
        return VisibilityInfo.FALLBACK;
    }
}
