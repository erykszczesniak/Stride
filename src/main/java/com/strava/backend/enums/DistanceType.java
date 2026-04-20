package com.strava.backend.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DistanceType {
    // Swimming pool distances
    FIFTY_M(0.05, "50 m"),
    HUNDRED_M(0.1, "100 m"),
    TWO_HUNDRED_M(0.2, "200 m"),
    FOUR_HUNDRED_M(0.4, "400 m"),

    // Shared (runs, bikes, long swims)
    ONE_KM(1.0, "1 km"),
    THREE_KM(3.0, "3 km"),
    FIVE_KM(5.0, "5 km"),
    TEN_KM(10.0, "10 km"),

    // Running-only
    HALF_MARATHON(21.0975, "Half Marathon"),
    MARATHON(42.195, "Marathon");

    private final double distanceKm;
    private final String displayName;
}
