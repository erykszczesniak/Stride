package com.strava.shared.events;

import java.time.Instant;

public record ActivityLikedEvent(
        Long activityId,
        Long activityOwnerId,
        Long likerUserId,
        String likerDisplayName,
        Instant occurredAt
) {}
