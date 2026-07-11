package com.strava.backend.dto.pipeline;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * GET /pipeline/v1/metrics/load?range={4w|3m|1y} payload. CTL/ATL points are
 * sampled over the requested range; weekly volumes cover the trailing weeks
 * that the Load screen's stacked bars render.
 */
public record PipelineLoadDto(
        String range,
        List<MetricPoint> ctl,
        List<MetricPoint> atl,
        List<WeeklyVolume> weeklyVolumes,
        WeekSummary weekSummary
) {

    public record MetricPoint(
            OffsetDateTime date,
            double value
    ) {
    }

    public record WeeklyVolume(
            OffsetDateTime weekStart,
            double swimKm,
            double bikeKm,
            double runKm
    ) {
    }

    public record WeekSummary(
            int hoursSeconds,
            int hoursDeltaSeconds,
            int loadTss,
            int loadDeltaTss
    ) {
    }
}
