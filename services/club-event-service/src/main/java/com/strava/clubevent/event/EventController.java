package com.strava.clubevent.event;

import com.strava.clubevent.domain.EventParticipant;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService events;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventDto create(@AuthenticationPrincipal Long userId,
                           @Valid @RequestBody CreateEventRequest req) {
        return events.create(userId, req);
    }

    @GetMapping
    public Page<EventDto> upcoming(Pageable pageable) {
        return events.upcoming(pageable);
    }

    @GetMapping("/club/{clubId}")
    public Page<EventDto> byClub(@PathVariable Long clubId, Pageable pageable) {
        return events.forClub(clubId, pageable);
    }

    @GetMapping("/{id}")
    public EventDto get(@PathVariable Long id) {
        return events.get(id);
    }

    @PatchMapping("/{id}")
    public EventDto update(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                           @Valid @RequestBody UpdateEventRequest req) {
        return events.update(id, userId, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        events.delete(id, userId);
    }

    @PostMapping("/{id}/register")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        events.register(id, userId);
    }

    @DeleteMapping("/{id}/register")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregister(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        events.unregister(id, userId);
    }

    @GetMapping("/{id}/participants")
    public Page<EventParticipant> participants(@PathVariable Long id, Pageable pageable) {
        return events.listParticipants(id, pageable);
    }
}
