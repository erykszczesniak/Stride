package com.strava.pipeline.pipeline;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Local stand-in for the monolith's Activity entity: the slice of an activity
 * the pipeline math needs, mapped from activity-service payloads
 * (/api/v1/activities/my, /api/v1/activities/{id}, /internal/activities).
 * Streams arrive pre-parsed as arrays (activity-service serializes them that
 * way), so unlike main there is no JSON-string parsing step here.
 */
@Builder
public record ActivityData(
        Long id,
        Long userId,
        String title,
        ActivityType activityType,
        Double distance,          // km
        Integer duration,         // seconds
        Integer averageHeartRate,
        Integer maxHeartRate,
        Integer averagePower,
        LocalDateTime startTime,
        String routePolyline,
        double[] elevationProfile,
        long[] pointTimestamps
) {
}
