package com.strava.shared.events;

import java.time.Instant;

public record CommentAddedEvent(
        Long commentId,
        Long activityId,
        Long authorUserId,
        String preview,
        Instant occurredAt
) {}
