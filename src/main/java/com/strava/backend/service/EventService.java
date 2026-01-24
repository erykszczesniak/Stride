package com.strava.backend.service;

import com.strava.backend.domain.Event;
import com.strava.backend.dto.event.EventRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EventService {

    Event getEventById(Long eventId);

    Page<Event> getUpcomingPublicEvents(Pageable pageable);

    List<Event> getUpcomingEventsByUserId(Long userId);

    List<Event> getUpcomingEventsByUserId(Long userId, int limit);

    Event createEvent(Long userId, EventRequest request);

    Event updateEvent(Long eventId, Long userId, EventRequest request);

    void deleteEvent(Long eventId, Long userId);

    void registerForEvent(Long eventId, Long userId);

    void unregisterFromEvent(Long eventId, Long userId);

    boolean isUserRegistered(Long eventId, Long userId);
}
