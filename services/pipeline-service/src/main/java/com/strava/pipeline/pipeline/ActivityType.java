package com.strava.pipeline.pipeline;

/**
 * Mirror of the platform-wide activity-type enum (main + activity-service).
 * Remote payloads carry the enum name as a string; {@link #from(String)} maps
 * unknown future values to {@link #OTHER} so a new sport in activity-service
 * can never break the pipeline facade (it is simply not a tri discipline).
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
    OTHER;

    public static ActivityType from(String raw) {
        if (raw == null) {
            return OTHER;
        }
        try {
            return valueOf(raw);
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }
}
