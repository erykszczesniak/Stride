package com.strava.shared.events;

import java.time.Instant;

public record UserUnfollowedEvent(
        Long followerUserId,
        Long followeeUserId,
        Instant occurredAt
) {}
