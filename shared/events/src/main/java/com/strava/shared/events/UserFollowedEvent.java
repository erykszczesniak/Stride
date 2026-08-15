package com.strava.shared.events;

import java.time.Instant;

public record UserFollowedEvent(
        Long followerUserId,
        Long followeeUserId,
        String followerDisplayName,
        Instant occurredAt
) {}
