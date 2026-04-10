package com.strava.backend.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DistanceType {
    ONE_KM(1.0, "1 km"),
    FIVE_KM(5.0, "5 km"),
    TEN_KM(10.0, "10 km"),
    HALF_MARATHON(21.0975, "Half Marathon"),
    MARATHON(42.195, "Marathon");

    private final double distanceKm;
    private final String displayName;
}
