package com.strava.shared.events;

import java.time.Instant;

public record PersonalRecordAchievedEvent(
        Long userId,
        Long activityId,
        String sport,
        String distanceLabel,
        Long timeSeconds,
        Instant occurredAt
) {}
