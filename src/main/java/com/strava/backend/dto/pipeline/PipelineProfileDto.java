package com.strava.backend.dto.pipeline;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * GET /pipeline/v1/athlete/profile payload. Every field is required by the
 * client, so gaps in the source model are filled with derived or neutral
 * values (see PipelineQueryService for the exact fallbacks).
 */
public record PipelineProfileDto(
        Athlete athlete,
        SeasonStats seasonStats,
        SeasonGoal seasonGoal,
        ThresholdZones thresholdZones
) {

    public record Athlete(
            String name,
            String initials,
            String ageGroup,
            String city
    ) {
    }

    public record SeasonStats(
            int sessions,
            int hoursSeconds,
            int prsSet,
            double swimKm,
            double bikeKm,
            double runKm
    ) {
    }

    public record SeasonGoal(
            String title,
            OffsetDateTime date,
            int targetSeconds
    ) {
    }

    public record ThresholdZones(
            int cssPaceSecondsPer100m,
            int ftpWatts,
            int thresholdPaceSecondsPerKm
    ) {
    }
}
