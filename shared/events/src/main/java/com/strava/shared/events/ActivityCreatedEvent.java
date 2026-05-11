package com.strava.shared.events;

import java.time.Instant;

public record ActivityCreatedEvent(
        Long activityId,
        Long userId,
        String sport,
        Double distanceMeters,
        Long durationSeconds,
        Double elevationGainMeters,
        Instant startedAt,
        Instant occurredAt
) {}
