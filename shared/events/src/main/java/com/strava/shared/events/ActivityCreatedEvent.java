package com.strava.shared.events;

import java.time.Instant;

public record ActivityCreatedEvent(
        Long activityId,
        Long userId,
        String sport,
        String title,
        boolean isPrivate,
        Double distanceMeters,
        Long durationSeconds,
        Double elevationGainMeters,
        Instant startedAt,
        Instant occurredAt
) {}
