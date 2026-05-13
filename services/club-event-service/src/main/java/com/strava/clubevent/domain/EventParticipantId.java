package com.strava.clubevent.domain;

import java.io.Serializable;
import java.util.Objects;

public class EventParticipantId implements Serializable {

    private Long eventId;
    private Long userId;

    public EventParticipantId() {}
    public EventParticipantId(Long eventId, Long userId) {
        this.eventId = eventId;
        this.userId = userId;
    }

    public Long getEventId() { return eventId; }
    public Long getUserId() { return userId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventParticipantId other)) return false;
        return Objects.equals(eventId, other.eventId) && Objects.equals(userId, other.userId);
    }

    @Override
    public int hashCode() { return Objects.hash(eventId, userId); }
}
