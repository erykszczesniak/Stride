package com.strava.shared.events;

import java.time.Instant;

public record UserDeletedEvent(
        Long userId,
        Instant occurredAt
) {}
