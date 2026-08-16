package com.strava.pipeline.pipeline;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Local stand-in for the monolith's PersonalRecord entity, mapped from
 * activity-service's GET /api/v1/personal-records payload.
 */
@Builder
public record RecordData(
        Long id,
        ActivityType activityType,
        DistanceType distanceType,
        Integer timeInSeconds,
        Integer previousTimeInSeconds,
        LocalDateTime achievedAt
) {
}
