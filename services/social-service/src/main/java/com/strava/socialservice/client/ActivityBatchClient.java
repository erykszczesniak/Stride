package com.strava.socialservice.client;

import com.strava.grpc.activity.ActivityInternalServiceGrpc;
import com.strava.grpc.activity.ActivityPayload;
import com.strava.grpc.activity.GetActivitiesRequest;
import com.strava.grpc.activity.GetActivitiesResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Batch activity hydration over activity-service's cluster-internal gRPC API
 * (ActivityInternalService.GetActivities) — one call per feed page instead of
 * N+1 per row. The channel target is {@code discovery:///activity-service},
 * resolved from Eureka instance metadata ({@code gRPC.port}) by net.devh's
 * discovery name resolver; the call is wrapped in Resilience4j retry +
 * circuit breaker (instance "activity-service") exactly as the former REST
 * client was, so a StatusRuntimeException (UNAVAILABLE, DEADLINE_EXCEEDED, …)
 * is recorded as a failure and eventually opens the circuit.
 *
 * The return value distinguishes two very different situations:
 *   * {@code Optional.of(map)} — the call succeeded; an id absent from the
 *     map means activity-service no longer has that activity (deleted but
 *     the event not yet consumed; the server silently skips unknown ids) and
 *     the caller should DROP the row, exactly like main drops feed entries
 *     whose Activity lookup returns nothing;
 *   * {@code Optional.empty()} — activity-service is down / circuit open;
 *     the caller degrades to the local feed_items snapshot instead.
 */
@Slf4j
@Component
public class ActivityBatchClient {

    /**
     * Per-call deadline. Feed pages batch at most a handful of ids (page size
     * ≤ 10), so a healthy activity-service answers well inside 3s — mirrors
     * the old RestTemplate's 2s connect / 3s read timeouts, keeping slow
     * calls flowing into Resilience4j instead of parking a request thread.
     */
    private static final long DEADLINE_SECONDS = 3;

    /**
     * Package-private injection seam: net.devh populates it from the
     * "activity-service" channel config; tests assign an in-process stub.
     */
    @GrpcClient("activity-service")
    ActivityInternalServiceGrpc.ActivityInternalServiceBlockingStub activityStub;

    @CircuitBreaker(name = "activity-service", fallbackMethod = "fallback")
    @Retry(name = "activity-service")
    public Optional<Map<Long, InternalActivityDto>> activitiesById(Collection<Long> activityIds) {
        if (activityIds == null || activityIds.isEmpty()) {
            return Optional.of(Map.of());
        }
        GetActivitiesResponse response = activityStub
                .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
                .getActivities(GetActivitiesRequest.newBuilder().addAllIds(activityIds).build());
        return Optional.of(response.getActivitiesList().stream()
                .map(ActivityBatchClient::toDto)
                .collect(Collectors.toMap(InternalActivityDto::id, Function.identity(), (a, b) -> a)));
    }

    @SuppressWarnings("unused")
    private Optional<Map<Long, InternalActivityDto>> fallback(Collection<Long> activityIds, Throwable ex) {
        log.warn("ActivityBatchClient fallback for {} ids (cause: {})",
                activityIds == null ? 0 : activityIds.size(), ex.getMessage());
        return Optional.empty();
    }

    /**
     * Wire → DTO mapping, field-parity with the retired REST JSON payload:
     * proto3 optional unset → null; ISO-8601 strings → LocalDateTime; the
     * typed stream fields → primitive arrays, empty-repeated → null (the REST
     * producer sent null when no stream was stored); photos always a list
     * (the producer never sent null, empty meant "no photos").
     */
    private static InternalActivityDto toDto(ActivityPayload p) {
        return new InternalActivityDto(
                p.getId(),
                p.getUserId(),
                p.getTitle(),
                p.hasDescription() ? p.getDescription() : null,
                p.getActivityType(),
                p.hasDistance() ? p.getDistance() : null,
                p.hasDuration() ? p.getDuration() : null,
                p.hasElevationGain() ? p.getElevationGain() : null,
                p.hasCalories() ? p.getCalories() : null,
                p.hasAverageSpeed() ? p.getAverageSpeed() : null,
                p.hasMaxSpeed() ? p.getMaxSpeed() : null,
                p.hasAverageHeartRate() ? p.getAverageHeartRate() : null,
                p.hasMaxHeartRate() ? p.getMaxHeartRate() : null,
                p.hasAverageCadence() ? p.getAverageCadence() : null,
                p.hasAveragePower() ? p.getAveragePower() : null,
                parseDateTime(p.hasStartTime() ? p.getStartTime() : null),
                parseDateTime(p.hasEndTime() ? p.getEndTime() : null),
                p.hasStartLocation() ? p.getStartLocation() : null,
                p.hasEndLocation() ? p.getEndLocation() : null,
                p.hasRoutePolyline() ? p.getRoutePolyline() : null,
                p.getElevationProfileCount() == 0 ? null
                        : p.getElevationProfileList().stream().mapToDouble(Double::doubleValue).toArray(),
                p.getPointTimestampsCount() == 0 ? null
                        : p.getPointTimestampsList().stream().mapToLong(Long::longValue).toArray(),
                p.hasMapImageUrl() ? p.getMapImageUrl() : null,
                p.getIsPrivate(),
                List.copyOf(p.getPhotosList()),
                parseDateTime(p.hasCreatedAt() ? p.getCreatedAt() : null),
                parseDateTime(p.hasUpdatedAt() ? p.getUpdatedAt() : null));
    }

    private static LocalDateTime parseDateTime(String iso) {
        return iso == null ? null : LocalDateTime.parse(iso);
    }
}
