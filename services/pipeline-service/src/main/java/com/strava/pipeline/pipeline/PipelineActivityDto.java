package com.strava.pipeline.pipeline;

import java.time.OffsetDateTime;

/**
 * Activity list item of the pipeline contract (docs of the consuming iOS app:
 * triathlon tracker, GET /v1/activities). Times are seconds, distances km,
 * dates ISO-8601 with offset. Heart rates fall back to 0 when the source
 * activity has none recorded — the contract fields are non-optional.
 */
public record PipelineActivityDto(
        String id,
        String discipline,
        String title,
        OffsetDateTime startDate,
        int durationSeconds,
        double distanceKm,
        int avgHeartRate,
        int maxHeartRate,
        boolean hasGPS
) {
}
