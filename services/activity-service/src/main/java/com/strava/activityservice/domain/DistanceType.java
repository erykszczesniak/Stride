package com.strava.activityservice.domain;

/**
 * Canonical race distances PRs can be tracked against.
 *
 * Only carries data the backend actually computes against (distance in km).
 * Display names are intentionally NOT stored here — they're a presentation
 * concern and live as i18n keys on the frontend under
 * {@code personalRecords.distances.{enumName}}. Which distances apply to
 * which sport is decided by the per-sport matrix in PersonalRecordService.
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

    public final double distanceKm;

    DistanceType(double distanceKm) {
        this.distanceKm = distanceKm;
    }
}
