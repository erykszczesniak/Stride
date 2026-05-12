package com.strava.activityservice.domain;

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

    public boolean isDistanceSport() {
        return switch (this) {
            case RUN, RIDE, SWIM, WALK, HIKE, VIRTUAL_RIDE, VIRTUAL_RUN,
                 NORDIC_SKI, ROLLER_SKI, INLINE_SKATE, KAYAKING, ROWING -> true;
            default -> false;
        };
    }
}
