package com.strava.shared.events;

import java.time.Instant;

public record UserUpdatedEvent(
        Long userId,
        String displayName,
        String avatar,
        Instant occurredAt
) {}
