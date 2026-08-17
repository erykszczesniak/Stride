package com.strava.activityservice.internal;

import com.strava.activityservice.activity.ActivityService;
import com.strava.activityservice.domain.Activity;
import com.strava.activityservice.domain.ActivityRepository;
import com.strava.activityservice.util.StreamJson;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Single source of truth for the cluster-internal read surface. Both
 * transports — the (legacy) REST {@code /internal/activities} controller and
 * the gRPC {@code ActivityInternalService} — delegate here, so batch-fetch
 * mapping, stream parsing and aggregation semantics can never drift apart.
 */
@Service
@RequiredArgsConstructor
public class InternalActivityQueryService {

    private final ActivityRepository activityRepository;
    private final ActivityService activityService;

    /**
     * Batch hydration by id. Missing ids are silently skipped; order is
     * unspecified. The elevation-profile / point-timestamp JSON TEXT columns
     * are parsed into primitive arrays (null when absent or malformed), and
     * the lazy photo collection is materialized as a detached copy inside
     * this read-only transaction (open-in-view is off).
     */
    @Transactional(readOnly = true)
    public List<InternalActivityResponse> getActivities(List<Long> ids) {
        return activityRepository.findAllById(ids).stream()
                .map(a -> InternalActivityResponse.from(a,
                        StreamJson.parseElevationProfile(a.getElevationProfile()),
                        StreamJson.parsePointTimestamps(a.getPointTimestamps())))
                .toList();
    }

    /** Aggregated stats for a user over [from, to] — delegates to the same SQL the REST path uses. */
    public Map<String, Object> getRangeStats(Long userId, LocalDateTime from, LocalDateTime to) {
        return activityService.getRangeStats(userId, from, to);
    }

    /**
     * Server-side replacement for the pattern of paging the public endpoint
     * and summing client-side (challenge-goal-service's ActivityClient). The
     * aggregation semantics mirror that client loop exactly:
     * <ul>
     *   <li>window filter is on {@code startTime}, inclusive on both ends
     *       (SQL BETWEEN = the old {@code !isBefore(from) && !isAfter(to)});</li>
     *   <li>sport filter is a case-insensitive match against the
     *       {@link com.strava.activityservice.domain.ActivityType} name — an
     *       unknown filter value matches nothing (empty summary), it does not
     *       error;</li>
     *   <li>null metrics contribute 0 but the activity still counts.</li>
     * </ul>
     */
    @Transactional(readOnly = true)
    public UserActivitySummaryTotals summarize(Long userId, String sportFilter,
                                               LocalDateTime from, LocalDateTime to) {
        List<Activity> window = activityRepository.findByUserIdAndStartTimeBetween(userId, from, to);

        double distanceKm = 0;
        long durationSeconds = 0;
        double elevationMeters = 0;
        int count = 0;
        for (Activity a : window) {
            if (sportFilter != null
                    && (a.getActivityType() == null || !sportFilter.equalsIgnoreCase(a.getActivityType().name()))) {
                continue;
            }
            if (a.getDistance() != null) distanceKm += a.getDistance();
            if (a.getDuration() != null) durationSeconds += a.getDuration();
            if (a.getElevationGain() != null) elevationMeters += a.getElevationGain();
            count++;
        }
        return new UserActivitySummaryTotals(distanceKm, durationSeconds, elevationMeters, count);
    }

    /** Aggregated totals for one user's activity window. */
    public record UserActivitySummaryTotals(double totalDistanceKm,
                                            long totalDurationSeconds,
                                            double totalElevationMeters,
                                            int activityCount) {
    }
}
