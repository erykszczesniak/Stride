package com.strava.shared.events;

import java.time.Instant;

public record CommentAddedEvent(
        Long commentId,
        Long activityId,
        Long activityOwnerId,
        Long authorUserId,
        String authorDisplayName,
        String preview,
        Instant occurredAt
) {}
