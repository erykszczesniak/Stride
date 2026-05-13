package com.strava.clubevent.event;

import com.strava.clubevent.domain.EventType;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateEventRequest(
        @Size(max = 255) String name,
        @Size(max = 2000) String description,
        EventType eventType,
        LocalDateTime startTime,
        LocalDateTime endTime,
        @Size(max = 255) String location,
        @Positive Integer maxParticipants
) {}
