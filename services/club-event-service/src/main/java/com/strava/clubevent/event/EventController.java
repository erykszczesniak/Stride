package com.strava.clubevent.event;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Public contract identical to the monolith's EventController (/api/v1/events
 * — plain JSON array list envelopes, POST/DELETE register). Like main, there
 * is no participants listing endpoint.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Event> events = eventService.getUpcomingPublicEvents(pageable);
        List<EventResponse> response = events.stream()
                .map(e -> mapToResponse(e, userId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<EventResponse>> getUpcomingEvents(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 5) Pageable pageable) {
        Page<Event> events = eventService.getUpcomingPublicEvents(pageable);
        List<EventResponse> response = events.stream()
                .map(e -> mapToResponse(e, userId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEventById(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long eventId) {
        Event event = eventService.getEventById(eventId);
        return ResponseEntity.ok(mapToResponse(event, userId));
    }

    @GetMapping("/my")
    public ResponseEntity<List<EventResponse>> getMyUpcomingEvents(@AuthenticationPrincipal Long userId) {
        List<Event> events = eventService.getUpcomingEventsByUserId(userId);
        List<EventResponse> response = events.stream()
                .map(e -> mapToResponse(e, userId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody EventRequest request) {
        Event event = eventService.createEvent(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(event, userId));
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<EventResponse> updateEvent(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody EventRequest request) {
        Event event = eventService.updateEvent(eventId, userId, request);
        return ResponseEntity.ok(mapToResponse(event, userId));
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long eventId) {
        eventService.deleteEvent(eventId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{eventId}/register")
    public ResponseEntity<Map<String, String>> registerForEvent(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long eventId) {
        eventService.registerForEvent(eventId, userId);
        return ResponseEntity.ok(Map.of("message", "Successfully registered for the event"));
    }

    @DeleteMapping("/{eventId}/register")
    public ResponseEntity<Map<String, String>> unregisterFromEvent(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long eventId) {
        eventService.unregisterFromEvent(eventId, userId);
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
