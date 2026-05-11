package com.strava.shared.events;

import java.time.Instant;
import java.util.Map;

public record NotificationRequestedEvent(
        Long recipientUserId,
        String type,
        String title,
        String body,
        Map<String, String> metadata,
        Instant occurredAt
) {}
