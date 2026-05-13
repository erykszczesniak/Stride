package com.strava.challengegoal.client;

import java.io.Serializable;

/**
 * Aggregated activity stats for a user/sport/window — returned by the
 * remote call to activity-service and stored in the Redis cache.
 * Implements Serializable so Spring's RedisCacheManager can serialize it
 * via the default JDK serializer (we'll swap in a Jackson-based serializer
 * in Phase 9 along with the rest of observability hardening).
 */
public record ActivitySummary(
        double totalDistanceKm,
        long totalDurationSeconds,
        double totalElevationMeters,
        long activityCount
) implements Serializable {

    public static final ActivitySummary EMPTY = new ActivitySummary(0, 0, 0, 0);
}
