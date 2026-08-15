package com.strava.challengegoal.client;

import com.strava.shared.security.UserContext;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Calls activity-service via Spring Cloud LoadBalancer (lb://activity-service)
 * resolved through Eureka. Three layers of protection on the read path:
 *
 *   1. Spring Cache (@Cacheable) backed by Redis — typical hit returns
 *      without touching activity-service at all (TTL set in
 *      application.yml). Invalidated by ActivityCreated Kafka events.
 *   2. Resilience4j @Retry — short retries on transient failures.
 *   3. Resilience4j @CircuitBreaker — opens after sustained failures so we
 *      stop hammering an already-down dependency; falls back to
 *      ActivitySummary.EMPTY so the read-side endpoints stay up and
 *      progress just reports 0 until the activity-service is healthy.
 *
 * The user identity is propagated to activity-service via the X-User-Id
 * header — same trust model gateway → service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityClient {

    private final RestTemplate loadBalanced;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Cache key is the (user, sport, window) tuple — the same activity
     * summary is shared across a Goal and a Challenge that happen to span
     * the same window for the same user. Invalidation by ActivityCreated
     * event evicts the whole "activity-summary" cache, so over-sharing here
     * is fine and cheap.
     */
    @Cacheable(
            value = "activity-summary",
            key = "#userId + '|' + (#sportFilter ?: 'ALL') + '|' + #from + '|' + #to")
    @CircuitBreaker(name = "activity-service", fallbackMethod = "fallback")
    @Retry(name = "activity-service")
    public ActivitySummary summaryFor(Long userId,
                                      String sportFilter,
                                      LocalDateTime from,
                                      LocalDateTime to) {
        log.debug("Fetching activity summary userId={} sport={} from={} to={}",
                userId, sportFilter, from, to);

        // Propagate the calling user's identity to activity-service so its
        // header-trust filter accepts us. Falls back to the target userId
        // when no SecurityContext is present (background recomputation case).
        HttpHeaders headers = new HttpHeaders();
        headers.add(UserContext.HEADER_USER_ID, callerOr(userId));

        // Pull all activities in the window (paged); we sum locally. For very
        // active users this would need server-side aggregation in
        // activity-service — that's an obvious follow-up. The /api/v1 contract
        // returns a plain JSON array plus page-total-count/page-total-elements
        // headers (not a Spring Page envelope).
        String url = "lb://activity-service/api/v1/activities/user/" + userId
                + "?size=200&page=0";

        var response = loadBalanced.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );

        List<Map<String, Object>> content = response.getBody();
        if (content == null) return ActivitySummary.EMPTY;

        double dist = 0;
        long dur = 0;
        double elev = 0;
        long count = 0;
        for (Map<String, Object> a : content) {
            String aSport = (String) a.get("activityType");
            if (sportFilter != null && !sportFilter.equalsIgnoreCase(aSport)) continue;

            String start = (String) a.get("startTime");
            if (start == null) continue;
            LocalDateTime started = LocalDateTime.parse(start, ISO);
            if (started.isBefore(from) || started.isAfter(to)) continue;

            Number distance = (Number) a.get("distance");
            Number duration = (Number) a.get("duration");
            Number elevation = (Number) a.get("elevationGain");
            if (distance != null) dist += distance.doubleValue();
            if (duration != null) dur += duration.longValue();
            if (elevation != null) elev += elevation.doubleValue();
            count++;
        }
        return new ActivitySummary(dist, dur, elev, count);
    }

    @SuppressWarnings("unused")
    private ActivitySummary fallback(Long userId,
                                     String sportFilter,
                                     LocalDateTime from,
                                     LocalDateTime to,
                                     Throwable ex) {
        log.warn("ActivityClient fallback for userId={} (cause: {})", userId, ex.getMessage());
        return ActivitySummary.EMPTY;
    }

    private String callerOr(Long fallback) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long id) return id.toString();
        return fallback.toString();
    }
}
