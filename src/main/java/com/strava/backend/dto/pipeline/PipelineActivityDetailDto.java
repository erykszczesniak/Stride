package com.strava.backend.dto.pipeline;

import java.util.List;

/**
 * GET /pipeline/v1/activities/{id} payload. Series are evenly spaced samples
 * over distance; pace units are discipline-specific (sec/km for runs, km/h for
 * rides, sec/100 m for swims). Splits carry the cumulative end-of-split
 * distance and the segment duration (not cumulative).
 *
 * {@code routePolyline} is always a Google Encoded Polyline (legacy JSON-array
 * rows are re-encoded server-side) or null when the activity has no usable
 * track. {@code elevationSeries} is the per-point altitude profile downsampled
 * for charting; empty when none is stored.
 */
public record PipelineActivityDetailDto(
        PipelineActivityDto activity,
        List<Double> paceSeries,
        List<Double> heartRateSeries,
        List<Split> splits,
        String routePolyline,
        List<Double> elevationSeries
) {

    public record Split(
            int distanceMeters,
            int durationSeconds,
            int avgHeartRate
    ) {
    }
}
