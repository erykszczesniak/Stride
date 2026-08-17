package com.strava.userservice.client;

import com.strava.grpc.activity.ActivityInternalServiceGrpc;
import com.strava.grpc.activity.ActivityStatsRange;
import com.strava.grpc.activity.GetStatsRangeRequest;
import com.strava.shared.security.UserContext;
import com.strava.userservice.user.ActivityStatsPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Activity data for profiles/admin views, composed from activity-service.
 *
 * Transport split (Phase 23c): totals ride the cluster-internal gRPC surface
 * ({@code ActivityInternalService.GetStatsRange}) over the same all-time window
 * the retired REST internal call used. Recent activities DELIBERATELY stay on
 * the public REST endpoint with the viewer's id forwarded as X-User-Id: they
 * consume the public {@code ActivityResponse} shape including social enrichment
 * — that is the public contract, not an internal API — and activity-service
 * applies its own visibility rules to the forwarded viewer.
 *
 * Both calls degrade to the stub behavior (zeros / empty list) when
 * activity-service is unavailable — a profile with missing stats beats a 502.
 * The module's Resilience4j config keeps the default record-everything
 * behavior (no recordExceptions list), so gRPC {@link io.grpc.StatusRuntimeException}s
 * count toward the {@code activity-service} circuit breaker exactly like the
 * REST client errors did.
 */
@Slf4j
@Component
@Profile("!standalone")
@RequiredArgsConstructor
public class ActivityStatsPortAdapter implements ActivityStatsPort {

    /** All-time window, identical to the one the old REST internal stats call used. */
    private static final String ALL_TIME_FROM = "1970-01-01T00:00:00";
    private static final String ALL_TIME_TO = "2999-01-01T00:00:00";

    private static final long STATS_DEADLINE_SECONDS = 3;

    private static final String RECENT_URL =
            "lb://activity-service/api/v1/activities/user/{userId}?page=0&size=5";

    private final RestTemplate loadBalanced;

    /**
     * Injected by net.devh from {@code grpc.client.activity-service.*}
     * (discovery:///activity-service). Package-private, non-final: unit tests
     * point it at an in-process server instead of Mockito-mocking generated code.
     */
    @GrpcClient("activity-service")
    ActivityInternalServiceGrpc.ActivityInternalServiceBlockingStub activityInternalStub;

    @Override
    @CircuitBreaker(name = "activity-service", fallbackMethod = "totalsFallback")
    @Retry(name = "activity-service")
    public ActivityTotals totalsFor(Long userId) {
        ActivityStatsRange stats = activityInternalStub
                .withDeadlineAfter(STATS_DEADLINE_SECONDS, TimeUnit.SECONDS)
                .getStatsRange(GetStatsRangeRequest.newBuilder()
                        .setUserId(userId)
                        .setFrom(ALL_TIME_FROM)
                        .setTo(ALL_TIME_TO)
                        .build());
        return new ActivityTotals(
                stats.getActivityCount(),
                stats.getTotalDistanceKm(),
                stats.getTotalDurationSeconds(),
                stats.getTotalElevationGain(),
                stats.getTotalCalories());
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
}
