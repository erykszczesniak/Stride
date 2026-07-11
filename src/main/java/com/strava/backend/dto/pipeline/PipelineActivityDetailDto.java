package com.strava.backend.dto.pipeline;

import java.util.List;

/**
 * GET /pipeline/v1/activities/{id} payload. Series are evenly spaced samples
 * over distance; pace units are discipline-specific (sec/km for runs, km/h for
 * rides, sec/100 m for swims). Splits carry the cumulative end-of-split
 * distance and the segment duration (not cumulative).
 */
public record PipelineActivityDetailDto(
        PipelineActivityDto activity,
        List<Double> paceSeries,
        List<Double> heartRateSeries,
        List<Split> splits
) {

    public record Split(
            int distanceMeters,
            int durationSeconds,
            int avgHeartRate
    ) {
    }
}
