package com.strava.clubevent.event;

import com.strava.clubevent.domain.EventType;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record CreateEventRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 2000) String description,
        @NotNull EventType eventType,
        @NotNull @Future LocalDateTime startTime,
        LocalDateTime endTime,
        @Size(max = 255) String location,
        Long clubId,
        @Positive Integer maxParticipants
) {}
