package com.strava.backend.dto.pipeline;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * GET /pipeline/v1/dashboard payload. {@code todayWorkout} is null — Stride
 * has no per-day planned workouts to project; the client renders its rest-day
 * state for null.
 */
public record PipelineDashboardDto(
        Form form,
        TodayWorkout todayWorkout,
        List<SportSnapshot> snapshots,
        List<PipelineActivityDto> recentActivities
) {

    public record Form(
            OffsetDateTime date,
            int ctl,
            int atl,
            int tsb
    ) {
    }

    public record TodayWorkout(
            String discipline,
            String title,
            String detail,
            int durationSeconds,
            double distanceKm,
            int tss,
            int projectedTsbAfter
    ) {
    }

    public record SportSnapshot(
            String discipline,
            double weekDistanceKm,
            int trendPercent
    ) {
    }
}
