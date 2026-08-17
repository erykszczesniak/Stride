package com.strava.challengegoal.client;

import com.strava.grpc.activity.ActivityInternalServiceGrpc;
import com.strava.grpc.activity.GetUserActivitySummaryRequest;
import com.strava.grpc.activity.UserActivitySummary;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Calls activity-service's cluster-internal gRPC endpoint
 * ({@code ActivityInternalService.GetUserActivitySummary}), resolved through
 * Eureka by the net.devh discovery name resolver (which reads the
 * {@code gRPC.port} instance metadata the server publishes). The old REST
 * page-and-sum is now a single server-side aggregation with identical
 * semantics: inclusive startTime window, case-insensitive sport filter,
 * null metrics count as 0 but the activity still counts.
 *
 * Three layers of protection on the read path:
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
 * A per-call deadline caps how long a sick dependency can hold a thread —
 * the resulting DEADLINE_EXCEEDED StatusRuntimeException is a recorded
 * exception for the circuit breaker. No caller identity is propagated:
 * the internal gRPC port is never routed by the gateway, so the surface is
 * cluster-trusted by construction (the REST-era X-User-Id header is gone).
 */
@Slf4j
@Component
public class ActivityClient {

    private static final long DEADLINE_SECONDS = 3;

    @GrpcClient("activity-service")
    private ActivityInternalServiceGrpc.ActivityInternalServiceBlockingStub activityStub;

    /** Spring path — the stub is injected into the field by net.devh. */
    ActivityClient() {
    }

    /** Test seam — lets unit tests hand in a stub bound to an in-process channel. */
    ActivityClient(ActivityInternalServiceGrpc.ActivityInternalServiceBlockingStub activityStub) {
        this.activityStub = activityStub;
    }

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

        // LocalDateTime.toString() emits ISO-8601, exactly what the server's
        // LocalDateTime.parse() expects on the other side of the wire.
        GetUserActivitySummaryRequest.Builder request = GetUserActivitySummaryRequest.newBuilder()
                .setUserId(userId)
                .setFrom(from.toString())
                .setTo(to.toString());
        if (sportFilter != null) request.setSportFilter(sportFilter);

        UserActivitySummary summary = activityStub
                .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
                .getUserActivitySummary(request.build());

        return new ActivitySummary(
                summary.getTotalDistanceKm(),
                summary.getTotalDurationSeconds(),
                summary.getTotalElevationMeters(),
                summary.getActivityCount());
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
}
