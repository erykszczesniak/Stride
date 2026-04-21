package com.strava.backend.enums

/**
 * Canonical race distances PRs can be tracked against.
 *
 * Only carries data the backend actually computes against (distance in km).
 * Display names are intentionally NOT stored here — they're a presentation
 * concern and live as i18n keys on the frontend under
 * `personalRecords.distances.{enumName}` so each of the 6 supported locales
 * can render "Half Marathon" / "Semi-Marathon" / "Полумарафон" correctly.
 * Backend API responses ship the stable enum name; the frontend localizes it.
 */
enum class DistanceType(val distanceKm: Double) {
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
}
