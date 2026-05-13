package com.strava.clubevent.event;

import com.strava.clubevent.domain.Event;
import com.strava.clubevent.domain.EventType;

import java.time.LocalDateTime;

public record EventDto(
        Long id,
        String name,
        String description,
        EventType eventType,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String location,
        Long clubId,
        Long organizerUserId,
        Integer maxParticipants,
        long registeredCount,
        LocalDateTime createdAt
) {
    public static EventDto from(Event e, long registered) {
        return new EventDto(e.getId(), e.getName(), e.getDescription(), e.getEventType(),
                e.getStartTime(), e.getEndTime(), e.getLocation(),
                e.getClubId(), e.getOrganizerUserId(), e.getMaxParticipants(),
                registered, e.getCreatedAt());
    }
}
