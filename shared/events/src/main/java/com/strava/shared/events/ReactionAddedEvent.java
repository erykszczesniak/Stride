package com.strava.shared.events;

import java.time.Instant;

public record ReactionAddedEvent(
        Long activityId,
        Long userId,
        String reactionType,
        Instant occurredAt
) {}
