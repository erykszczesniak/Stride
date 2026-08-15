package com.strava.shared.events;

import java.time.Instant;

public record ActivityVisibilityChangedEvent(
        Long activityId,
        Long userId,
        boolean nowPrivate,
        Instant activityCreatedAt,
        Instant occurredAt
) {}
