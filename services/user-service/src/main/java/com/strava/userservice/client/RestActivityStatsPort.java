package com.strava.userservice.client;

import com.strava.shared.security.UserContext;
import com.strava.userservice.user.ActivityStatsPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Activity data for profiles/admin views, composed from activity-service.
 *
 * Totals use the internal stats API over an all-time window; recent activities
 * ride the public endpoint with the viewer's id forwarded as X-User-Id so
 * activity-service applies its own visibility rules. Both calls degrade to the
 * stub behavior (zeros / empty list) when activity-service is unavailable —
 * a profile with missing stats beats a 502.
 */
@Slf4j
@Component
@Profile("!standalone")
@RequiredArgsConstructor
public class RestActivityStatsPort implements ActivityStatsPort {

    private static final String STATS_URL =
            "lb://activity-service/internal/activities/stats/range"
                    + "?userId={userId}&from=1970-01-01T00:00:00&to=2999-01-01T00:00:00";
    private static final String RECENT_URL =
            "lb://activity-service/api/v1/activities/user/{userId}?page=0&size=5";

    private final RestTemplate loadBalanced;

    @Override
    @CircuitBreaker(name = "activity-service", fallbackMethod = "totalsFallback")
    @Retry(name = "activity-service")
    public ActivityTotals totalsFor(Long userId) {
        Map<String, Object> body = loadBalanced.exchange(
                STATS_URL, HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<Map<String, Object>>() {}, userId).getBody();
        if (body == null) {
            return ActivityTotals.empty();
        }
        return new ActivityTotals(
                asInt(body.get("activityCount")),
                asDouble(body.get("totalDistanceKm")),
                asLong(body.get("totalDurationSeconds")),
                asLong(body.get("totalElevationGain")),
                asLong(body.get("totalCalories")));
    }

    @Override
    @CircuitBreaker(name = "activity-service", fallbackMethod = "recentFallback")
    @Retry(name = "activity-service")
    public List<Object> recentPublicActivities(Long userId, Long viewerId) {
        HttpHeaders headers = new HttpHeaders();
        if (viewerId != null) {
            headers.add(UserContext.HEADER_USER_ID, String.valueOf(viewerId));
        }
        List<Object> body = loadBalanced.exchange(
                RECENT_URL, HttpMethod.GET, new HttpEntity<>(headers),
                new ParameterizedTypeReference<List<Object>>() {}, userId).getBody();
        return body != null ? body : List.of();
    }

    @SuppressWarnings("unused")
    private ActivityTotals totalsFallback(Long userId, Throwable cause) {
        log.warn("activity-service stats unavailable for userId={}: {}", userId, cause.getMessage());
        return ActivityTotals.empty();
    }

    @SuppressWarnings("unused")
    private List<Object> recentFallback(Long userId, Long viewerId, Throwable cause) {
        log.warn("activity-service recent activities unavailable for userId={}: {}", userId, cause.getMessage());
        return List.of();
    }

    private static int asInt(Object v) {
        return v instanceof Number n ? n.intValue() : 0;
    }

    private static long asLong(Object v) {
        return v instanceof Number n ? n.longValue() : 0L;
    }

    private static double asDouble(Object v) {
        return v instanceof Number n ? n.doubleValue() : 0.0;
    }
}
