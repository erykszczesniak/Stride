package com.strava.shared.events;

import java.time.Instant;

public record ActivityDeletedEvent(
        Long activityId,
        Long userId,
        Instant occurredAt
) {}
