package com.strava.backend.controller;

import com.strava.backend.domain.Event;
import com.strava.backend.dto.event.EventRequest;
import com.strava.backend.dto.event.EventResponse;
import com.strava.backend.security.UserPrincipal;
import com.strava.backend.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.strava.backend.constants.PathConstants.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(API_V1_EVENTS)
public class EventController {

    private final EventService eventService;

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) authentication.getPrincipal()).getId();
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getUpcomingEvents(
            @PageableDefault(size = 20) Pageable pageable) {
        Long currentUserId = getAuthenticatedUserId();
        Page<Event> events = eventService.getUpcomingPublicEvents(pageable);
        List<EventResponse> response = events.stream()
                .map(e -> mapToResponse(e, currentUserId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping(EVENT_ID)
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long eventId) {
        Long currentUserId = getAuthenticatedUserId();
        Event event = eventService.getEventById(eventId);
        return ResponseEntity.ok(mapToResponse(event, currentUserId));
    }

    @GetMapping("/my")
    public ResponseEntity<List<EventResponse>> getMyUpcomingEvents() {
        Long currentUserId = getAuthenticatedUserId();
        List<Event> events = eventService.getUpcomingEventsByUserId(currentUserId);
        List<EventResponse> response = events.stream()
                .map(e -> mapToResponse(e, currentUserId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventRequest request) {
        Long currentUserId = getAuthenticatedUserId();
        Event event = eventService.createEvent(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(event, currentUserId));
    }

    @PutMapping(EVENT_ID)
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody EventRequest request) {
        Long currentUserId = getAuthenticatedUserId();
        Event event = eventService.updateEvent(eventId, currentUserId, request);
        return ResponseEntity.ok(mapToResponse(event, currentUserId));
    }

    @DeleteMapping(EVENT_ID)
    public ResponseEntity<Void> deleteEvent(@PathVariable Long eventId) {
        Long currentUserId = getAuthenticatedUserId();
        eventService.deleteEvent(eventId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(EVENT_ID + REGISTER_EVENT)
    public ResponseEntity<Map<String, String>> registerForEvent(@PathVariable Long eventId) {
        Long currentUserId = getAuthenticatedUserId();
        eventService.registerForEvent(eventId, currentUserId);
        return ResponseEntity.ok(Map.of("message", "Successfully registered for the event"));
    }

    @DeleteMapping(EVENT_ID + REGISTER_EVENT)
    public ResponseEntity<Map<String, String>> unregisterFromEvent(@PathVariable Long eventId) {
        Long currentUserId = getAuthenticatedUserId();
        eventService.unregisterFromEvent(eventId, currentUserId);
        return ResponseEntity.ok(Map.of("message", "Successfully unregistered from the event"));
    }

    private EventResponse mapToResponse(Event event, Long currentUserId) {
        EventResponse response = new EventResponse();
        response.setId(event.getId());
        response.setName(event.getName());
        response.setDescription(event.getDescription());
        response.setCoverImage(event.getCoverImage());
        response.setActivityType(event.getActivityType());
        response.setEventDate(event.getEventDate());
        response.setLocation(event.getLocation());
        response.setCity(event.getCity());
        response.setDistance(event.getDistance());
        response.setRegisteredCount(event.getRegisteredCount());
        response.setMaxParticipants(event.getMaxParticipants());
        response.setPublic(event.isPublic());
        response.setRegistered(eventService.isUserRegistered(event.getId(), currentUserId));
        return response;
    }
}
