package com.strava.pipeline.pipeline;

/**
 * Mirror of the canonical PR race distances (main + activity-service). Only
 * carries the km value the pipeline sorts records by; labels are derived in
 * {@link PipelineQueryService#eventLabel(DistanceType)}.
 */
public enum DistanceType {
    // Swimming pool distances
    FIFTY_M(0.05),
    HUNDRED_M(0.1),
    TWO_HUNDRED_M(0.2),
    FOUR_HUNDRED_M(0.4),

    // Shared (runs, bikes, long swims)
    ONE_KM(1.0),
    THREE_KM(3.0),
    FIVE_KM(5.0),
    TEN_KM(10.0),

    // Running-only
    HALF_MARATHON(21.0975),
    MARATHON(42.195);

    private final double distanceKm;

    DistanceType(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public double getDistanceKm() {
        return distanceKm;
    }
}
