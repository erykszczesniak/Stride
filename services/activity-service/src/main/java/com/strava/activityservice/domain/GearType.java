package com.strava.activityservice.domain;

/**
 * Mirrors main's GearType. Gear tracking itself lands in a later phase —
 * the enum already exists here because ActivityGearSummary (a nested field
 * of every activity response) references it.
 */
public enum GearType {
    SHOES,
    BIKE,
    OTHER
}
