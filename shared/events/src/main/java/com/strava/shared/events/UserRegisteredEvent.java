package com.strava.shared.events;

import java.time.Instant;

public record UserRegisteredEvent(
        Long userId,
        String email,
        String displayName,
        Instant occurredAt
) {}
