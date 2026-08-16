package com.strava.pipeline.client;

import com.strava.pipeline.exception.ApiRequestException;
import com.strava.pipeline.pipeline.ActivityData;
import com.strava.pipeline.pipeline.ActivityType;
import com.strava.pipeline.pipeline.DistanceType;
import com.strava.pipeline.pipeline.RecordData;
import com.strava.shared.security.UserContext;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Activity data for the pipeline facade, composed over activity-service via
 * Spring Cloud LoadBalancer (lb://activity-service) + Resilience4j.
 *
 * Listing goes through the public {@code /api/v1/activities/my} contract with
 * the authenticated user propagated as X-User-Id (same gateway→service trust
 * model). That payload already carries the GPS streams (polyline, per-point
 * timestamps, elevation profile), so splits/series need no second call.
 * Activity detail uses {@code /api/v1/activities/{id}} so activity-service's
 * ActivityVisibility applies exactly main's rule (own always; foreign only
 * when public and the owner's profile is visible — the #119/#123 fix); its
 * 404-on-deny maps 1:1 onto the pipeline contract's not_found. Average power
 * (FTP derivation only) is enriched via the cluster-internal batch API since
 * the public list payload does not carry it.
 *
 * Failures of these primary sources surface as the pipeline contract's
 * server_error (main can only miss this data when the request itself is bad);
 * the power enrichment degrades to "no power data" like main does for
 * activities without power.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityClient {

    /** Fetch-everything page: the pipeline aggregates the full history. */
    private static final int PAGE_SIZE = 1000;

    private final RestTemplate loadBalanced;

    // --- activities ------------------------------------------------------

    @CircuitBreaker(name = "activity-service", fallbackMethod = "myActivitiesFallback")
    @Retry(name = "activity-service")
    public List<ActivityData> myActivities(Long userId) {
        String url = "lb://activity-service/api/v1/activities/my?page=0&size=" + PAGE_SIZE
                + "&sort=createdAt,desc";
        var response = loadBalanced.exchange(url, HttpMethod.GET, entityFor(userId),
                new ParameterizedTypeReference<List<ActivityPayload>>() {
                });
        List<ActivityPayload> body = response.getBody();
        if (body == null) {
            return List.of();
        }
        return body.stream().map(ActivityPayload::toData).collect(Collectors.toList());
    }

    @SuppressWarnings("unused")
    private List<ActivityData> myActivitiesFallback(Long userId, Throwable ex) {
        throw serverError("Activity data unavailable", userId, ex);
    }

    @CircuitBreaker(name = "activity-service", fallbackMethod = "activityDetailFallback")
    @Retry(name = "activity-service")
    public ActivityData activityDetail(Long userId, Long activityId) {
        String url = "lb://activity-service/api/v1/activities/" + activityId;
        try {
            var response = loadBalanced.exchange(url, HttpMethod.GET, entityFor(userId),
                    ActivityPayload.class);
            ActivityPayload body = response.getBody();
            if (body == null) {
                throw new ApiRequestException("Activity not found", HttpStatus.NOT_FOUND);
            }
            return body.toData();
        } catch (HttpClientErrorException.NotFound e) {
            // activity-service 404s both unknown ids and visibility denials —
            // exactly the shape main's resolveViewableActivity produced.
            throw new ApiRequestException("Activity not found", HttpStatus.NOT_FOUND);
        }
    }

    @SuppressWarnings("unused")
    private ActivityData activityDetailFallback(Long userId, Long activityId, Throwable ex) {
        if (ex instanceof ApiRequestException api) {
            throw api;
        }
        throw serverError("Activity data unavailable", userId, ex);
    }

    /**
     * Average power by activity id via the cluster-internal batch API (the
     * public list payload has no power field). Empty on failure — the FTP
     * zone then reports "not set" (0), main's behavior for power-less data.
     */
    @CircuitBreaker(name = "activity-service", fallbackMethod = "averagePowerFallback")
    @Retry(name = "activity-service")
    public Map<Long, Integer> averagePowerByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        String joined = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        String url = "lb://activity-service/internal/activities?ids=" + joined;
        var response = loadBalanced.exchange(url, HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<InternalActivityPayload>>() {
                });
        List<InternalActivityPayload> body = response.getBody();
        if (body == null) {
            return Map.of();
        }
        Map<Long, Integer> powers = new HashMap<>();
        for (InternalActivityPayload payload : body) {
            if (payload.id() != null && payload.averagePower() != null) {
                powers.put(payload.id(), payload.averagePower());
            }
        }
        return powers;
    }

    @SuppressWarnings("unused")
    private Map<Long, Integer> averagePowerFallback(List<Long> ids, Throwable ex) {
        log.warn("ActivityClient power enrichment fallback (cause: {})", ex.getMessage());
        return Map.of();
    }

    // --- personal records ------------------------------------------------

    @CircuitBreaker(name = "activity-service", fallbackMethod = "personalRecordsFallback")
    @Retry(name = "activity-service")
    public List<RecordData> personalRecords(Long userId) {
        var response = loadBalanced.exchange("lb://activity-service/api/v1/personal-records",
                HttpMethod.GET, entityFor(userId),
                new ParameterizedTypeReference<List<PersonalRecordPayload>>() {
                });
        List<PersonalRecordPayload> body = response.getBody();
        if (body == null) {
            return List.of();
        }
        return body.stream()
                .map(PersonalRecordPayload::toData)
                .filter(record -> record.distanceType() != null)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unused")
    private List<RecordData> personalRecordsFallback(Long userId, Throwable ex) {
        throw serverError("Personal records unavailable", userId, ex);
    }

    // --- helpers ---------------------------------------------------------

    private static HttpEntity<Void> entityFor(Long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(UserContext.HEADER_USER_ID, userId.toString());
        return new HttpEntity<>(headers);
    }

    private ApiRequestException serverError(String message, Long userId, Throwable ex) {
        log.warn("ActivityClient failure for userId={} ({}: {})",
                userId, message, ex.getMessage());
        return new ApiRequestException(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // --- remote payload shapes (unknown fields ignored) ------------------

    record ActivityPayload(
            Long id,
            String title,
            String activityType,
            Double distance,
            Integer duration,
            Integer averageHeartRate,
            Integer maxHeartRate,
            Integer averagePower,
            LocalDateTime startTime,
            String routePolyline,
            double[] elevationProfile,
            long[] pointTimestamps
    ) {
        ActivityData toData() {
            return new ActivityData(id, null, title, ActivityType.from(activityType),
                    distance, duration, averageHeartRate, maxHeartRate, averagePower,
                    startTime, routePolyline, elevationProfile, pointTimestamps);
        }
    }

    record InternalActivityPayload(Long id, Integer averagePower) {
    }

    record PersonalRecordPayload(
            Long id,
            String activityType,
            String distanceType,
            Integer timeInSeconds,
            Integer previousTimeInSeconds,
            LocalDateTime achievedAt
    ) {
        RecordData toData() {
            return new RecordData(id, ActivityType.from(activityType),
                    parseDistance(distanceType), timeInSeconds, previousTimeInSeconds, achievedAt);
        }

        private static DistanceType parseDistance(String raw) {
            if (raw == null) {
                return null;
            }
            try {
                return DistanceType.valueOf(raw);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }
}
