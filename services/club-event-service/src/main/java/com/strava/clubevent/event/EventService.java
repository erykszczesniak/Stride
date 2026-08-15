package com.strava.clubevent.event;

import com.strava.clubevent.club.ClubRepository;
import com.strava.clubevent.exception.BadRequestException;
import com.strava.clubevent.exception.ForbiddenException;
import com.strava.clubevent.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Port of the monolith's EventServiceImpl — same registration guards
 * (already-registered / full / deadline) and the same error messages, over an
 * id-based registration set.
 */
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final ClubRepository clubRepository;

    @Transactional(readOnly = true)
    public Event getEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
    }

    @Transactional(readOnly = true)
    public Page<Event> getUpcomingPublicEvents(Pageable pageable) {
        return eventRepository.findUpcomingPublicEvents(LocalDateTime.now(), pageable);
    }

    @Transactional(readOnly = true)
    public List<Event> getUpcomingEventsByUserId(Long userId) {
        return eventRepository.findUpcomingEventsByUserId(userId, LocalDateTime.now());
    }

    @Transactional
    public Event createEvent(Long userId, EventRequest request) {
        Long clubId = null;
        if (request.getClubId() != null) {
            clubId = clubRepository.findById(request.getClubId())
                    .orElseThrow(() -> new NotFoundException("Club not found"))
                    .getId();
        }

        Event event = Event.builder()
                .name(request.getName())
                .description(request.getDescription())
                .coverImage(request.getCoverImage())
                .activityType(request.getActivityType())
                .eventDate(request.getEventDate())
                .registrationDeadline(request.getRegistrationDeadline())
                .location(request.getLocation())
                .city(request.getCity())
                .country(request.getCountry())
                .distance(request.getDistance())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .maxParticipants(request.getMaxParticipants())
                .entryFee(request.getEntryFee())
                .currency(request.getCurrency())
                .externalUrl(request.getExternalUrl())
                .isPublic(request.isPublic())
                .organizerId(userId)
                .clubId(clubId)
                .build();

        return eventRepository.save(event);
    }

    @Transactional
    public Event updateEvent(Long eventId, Long userId, EventRequest request) {
        Event event = getEventById(eventId);

        if (!event.getOrganizerId().equals(userId)) {
            throw new ForbiddenException("You don't have permission to update this event");
        }

        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setCoverImage(request.getCoverImage());
        event.setActivityType(request.getActivityType());
        event.setEventDate(request.getEventDate());
        event.setRegistrationDeadline(request.getRegistrationDeadline());
        event.setLocation(request.getLocation());
        event.setCity(request.getCity());
        event.setCountry(request.getCountry());
        event.setDistance(request.getDistance());
        event.setLatitude(request.getLatitude());
        event.setLongitude(request.getLongitude());
        event.setMaxParticipants(request.getMaxParticipants());
        event.setEntryFee(request.getEntryFee());
        event.setCurrency(request.getCurrency());
        event.setExternalUrl(request.getExternalUrl());
        event.setPublic(request.isPublic());

        return eventRepository.save(event);
    }

    @Transactional
    public void deleteEvent(Long eventId, Long userId) {
        Event event = getEventById(eventId);

        if (!event.getOrganizerId().equals(userId)) {
            throw new ForbiddenException("You don't have permission to delete this event");
        }

        eventRepository.delete(event);
    }

    @Transactional
    public void registerForEvent(Long eventId, Long userId) {
        Event event = getEventById(eventId);

        if (event.getRegisteredUserIds().contains(userId)) {
            throw new BadRequestException("You are already registered for this event");
        }

        if (event.isFull()) {
            throw new BadRequestException("This event is full");
        }

        if (!event.isRegistrationOpen()) {
            throw new BadRequestException("Registration for this event is closed");
        }

        event.getRegisteredUserIds().add(userId);
        eventRepository.save(event);
    }

    @Transactional
    public void unregisterFromEvent(Long eventId, Long userId) {
        Event event = getEventById(eventId);

        if (!event.getRegisteredUserIds().contains(userId)) {
            throw new BadRequestException("Not registered for this event");
        }

        event.getRegisteredUserIds().remove(userId);
        eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public boolean isUserRegistered(Long eventId, Long userId) {
        return eventRepository.isUserRegistered(eventId, userId);
    }
}
