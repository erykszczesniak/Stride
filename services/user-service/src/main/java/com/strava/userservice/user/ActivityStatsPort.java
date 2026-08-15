package com.strava.userservice.user;

import java.util.List;

/**
 * Activity data needed by user profiles and user/admin responses. Activities live in
 * activity-service; this port is the composition seam that a sync REST client
 * (Resilience4j-wrapped, Phase 13/22 of the parity plan) will implement. Until then the
 * {@link StubActivityStatsPort} keeps the contract shape with zeroed stats.
 */
public interface ActivityStatsPort {

    ActivityTotals totalsFor(Long userId);

    /** Recent public activities of {@code userId} as seen by {@code viewerId} ({@code ActivityResponse}-shaped). */
    List<Object> recentPublicActivities(Long userId, Long viewerId);

    default int countActivities(Long userId) {
        return totalsFor(userId).totalActivities();
    }

    record ActivityTotals(
            int totalActivities,
            double totalDistance,
            long totalDurationSeconds,
            long totalElevationMeters,
            long totalCalories
    ) {
        public static ActivityTotals empty() {
            return new ActivityTotals(0, 0.0, 0L, 0L, 0L);
        }
    }
}
