package com.strava.activityservice.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Canonical distances PRs are tracked against. Pool distances are swim-only;
 * the longer running distances (half marathon, marathon) are run-only.
 *
 * Display names are intentionally not stored here — they're a frontend i18n
 * concern under {@code personalRecords.distances.{name}}.
 */
public enum DistanceType {
    FIFTY_M(0.05),
    HUNDRED_M(0.10),
    TWO_HUNDRED_M(0.20),
    FOUR_HUNDRED_M(0.40),
    ONE_KM(1.0),
    THREE_KM(3.0),
    FIVE_KM(5.0),
    TEN_KM(10.0),
    HALF_MARATHON(21.0975),
    MARATHON(42.195);

    public final double distanceKm;

    DistanceType(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    private static final Set<DistanceType> SWIM_ONLY =
            EnumSet.of(FIFTY_M, HUNDRED_M, TWO_HUNDRED_M, FOUR_HUNDRED_M);

    private static final Set<DistanceType> RUN_ONLY =
            EnumSet.of(HALF_MARATHON, MARATHON);

    public boolean isApplicableTo(ActivityType sport) {
        if (SWIM_ONLY.contains(this)) return sport == ActivityType.SWIM;
        if (RUN_ONLY.contains(this)) return sport == ActivityType.RUN || sport == ActivityType.VIRTUAL_RUN;
        return sport.isDistanceSport();
    }
}
