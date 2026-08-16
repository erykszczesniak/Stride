package com.strava.webbff.report;

/**
 * Ported 1:1 from main — the report builder validates the {@code activityType}
 * argument with {@code valueOf} exactly like the monolith did, and
 * {@link PaceFormatter} keys its per-sport behavior off this enum.
 */
public enum ActivityType {
    RUN,
    RIDE,
    SWIM,
    WALK,
    HIKE,
    ALPINE_SKI,
    BACKCOUNTRY_SKI,
    CROSSFIT,
    ELLIPTICAL,
    ICE_SKATE,
    INLINE_SKATE,
    KAYAKING,
    KITESURF,
    NORDIC_SKI,
    ROCK_CLIMBING,
    ROLLER_SKI,
    ROWING,
    SNOWBOARD,
    SNOWSHOE,
    STAIR_STEPPER,
    STAND_UP_PADDLING,
    SURFING,
    VIRTUAL_RIDE,
    VIRTUAL_RUN,
    WEIGHT_TRAINING,
    WINDSURF,
    WORKOUT,
    YOGA,
    OTHER
}
