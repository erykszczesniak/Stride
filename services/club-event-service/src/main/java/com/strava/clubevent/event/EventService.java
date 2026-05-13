package com.strava.clubevent.event;

import com.strava.clubevent.domain.*;
import com.strava.clubevent.exception.BadRequestException;
import com.strava.clubevent.exception.ForbiddenException;
import com.strava.clubevent.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository events;
    private final EventParticipantRepository participants;
    private final ClubRepository clubs;

    @Transactional
    public EventDto create(Long userId, CreateEventRequest req) {
        if (req.clubId() != null && !clubs.existsById(req.clubId())) {
            throw new NotFoundException("Club " + req.clubId() + " not found");
        }
        Event e = Event.builder()
                .name(req.name())
                .description(req.description())
                .eventType(req.eventType())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .location(req.location())
                .clubId(req.clubId())
                .organizerUserId(userId)
                .maxParticipants(req.maxParticipants())
                .build();
        events.save(e);
        // Organizer is auto-registered.
        participants.save(EventParticipant.builder()
                .eventId(e.getId())
                .userId(userId)
                .status(ParticipantStatus.REGISTERED)
                .build());
        return EventDto.from(e, 1);
    }

    @Transactional(readOnly = true)
    public EventDto get(Long id) {
        Event e = events.findById(id).orElseThrow(() -> new NotFoundException("Event " + id + " not found"));
        return EventDto.from(e, participants.countByEventIdAndStatus(id, ParticipantStatus.REGISTERED));
    }

    @Transactional(readOnly = true)
    public Page<EventDto> upcoming(Pageable pageable) {
        return events.findAllByStartTimeAfterOrderByStartTimeAsc(LocalDateTime.now(), pageable)
                .map(e -> EventDto.from(e,
                        participants.countByEventIdAndStatus(e.getId(), ParticipantStatus.REGISTERED)));
    }

    @Transactional(readOnly = true)
    public Page<EventDto> forClub(Long clubId, Pageable pageable) {
        return events.findAllByClubIdOrderByStartTimeAsc(clubId, pageable)
                .map(e -> EventDto.from(e,
                        participants.countByEventIdAndStatus(e.getId(), ParticipantStatus.REGISTERED)));
    }

    @Transactional
    public EventDto update(Long eventId, Long userId, UpdateEventRequest req) {
        Event e = events.findById(eventId).orElseThrow(() -> new NotFoundException("Event " + eventId + " not found"));
        if (!e.getOrganizerUserId().equals(userId)) {
            throw new ForbiddenException("Only the organizer can modify the event");
        }
        if (req.name() != null) e.setName(req.name());
        if (req.description() != null) e.setDescription(req.description());
        if (req.eventType() != null) e.setEventType(req.eventType());
        if (req.startTime() != null) e.setStartTime(req.startTime());
        if (req.endTime() != null) e.setEndTime(req.endTime());
        if (req.location() != null) e.setLocation(req.location());
        if (req.maxParticipants() != null) e.setMaxParticipants(req.maxParticipants());
        return EventDto.from(e, participants.countByEventIdAndStatus(eventId, ParticipantStatus.REGISTERED));
    }

    @Transactional
    public void delete(Long eventId, Long userId) {
        Event e = events.findById(eventId).orElseThrow(() -> new NotFoundException("Event " + eventId + " not found"));
        if (!e.getOrganizerUserId().equals(userId)) {
            throw new ForbiddenException("Only the organizer can delete the event");
        }
        participants.deleteAll(participants.findAllByEventId(eventId, Pageable.unpaged()).getContent());
        events.delete(e);
    }

    @Transactional
    public void register(Long eventId, Long userId) {
        Event e = events.findById(eventId).orElseThrow(() -> new NotFoundException("Event " + eventId + " not found"));
        if (participants.existsByEventIdAndUserId(eventId, userId)) return; // idempotent
        if (e.getMaxParticipants() != null) {
            long count = participants.countByEventIdAndStatus(eventId, ParticipantStatus.REGISTERED);
            if (count >= e.getMaxParticipants()) {
                throw new BadRequestException("Event is full");
            }
        }
        participants.save(EventParticipant.builder()
                .eventId(eventId)
                .userId(userId)
                .status(ParticipantStatus.REGISTERED)
                .build());
    }

    @Transactional
    public void unregister(Long eventId, Long userId) {
        Event e = events.findById(eventId).orElseThrow(() -> new NotFoundException("Event " + eventId + " not found"));
        if (e.getOrganizerUserId().equals(userId)) {
            throw new ForbiddenException("Organizer cannot unregister — delete the event instead");
        }
        participants.deleteByEventIdAndUserId(eventId, userId);
    }

    @Transactional(readOnly = true)
    public Page<EventParticipant> listParticipants(Long eventId, Pageable pageable) {
        return participants.findAllByEventId(eventId, pageable);
    }
}
