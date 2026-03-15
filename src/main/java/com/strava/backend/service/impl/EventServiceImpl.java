package com.strava.backend.service.impl;

import com.strava.backend.domain.Club;
import com.strava.backend.domain.Event;
import com.strava.backend.domain.User;
import com.strava.backend.dto.event.EventRequest;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.repository.ClubRepository;
import com.strava.backend.repository.EventRepository;
import com.strava.backend.service.EventService;
import com.strava.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.strava.backend.constants.ErrorMessage.*;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final ClubRepository clubRepository;
    private final UserService userService;

    @Override
    @Transactional(readOnly = true)
    public Event getEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ApiRequestException(EVENT_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Event> getUpcomingPublicEvents(Pageable pageable) {
        return eventRepository.findUpcomingPublicEvents(LocalDateTime.now(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Event> getUpcomingEventsByUserId(Long userId) {
        return eventRepository.findUpcomingEventsByUserId(userId, LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Event> getUpcomingEventsByUserId(Long userId, int limit) {
        return eventRepository.findUpcomingEventsByUserId(userId, LocalDateTime.now(), PageRequest.of(0, limit))
                .getContent();
    }

    @Override
    @Transactional
    public Event createEvent(Long userId, EventRequest request) {
        User user = userService.getUserById(userId);

        Club club = null;
        if (request.getClubId() != null) {
            club = clubRepository.findById(request.getClubId())
                    .orElseThrow(() -> new ApiRequestException(CLUB_NOT_FOUND, HttpStatus.NOT_FOUND));
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
                .organizer(user)
                .club(club)
                .build();

        return eventRepository.save(event);
    }

    @Override
    @Transactional
    public Event updateEvent(Long eventId, Long userId, EventRequest request) {
        Event event = getEventById(eventId);

        if (!event.getOrganizer().getId().equals(userId)) {
            throw new ApiRequestException("You don't have permission to update this event", HttpStatus.FORBIDDEN);
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

    @Override
    @Transactional
    public void deleteEvent(Long eventId, Long userId) {
        Event event = getEventById(eventId);

        if (!event.getOrganizer().getId().equals(userId)) {
            throw new ApiRequestException("You don't have permission to delete this event", HttpStatus.FORBIDDEN);
        }

        eventRepository.delete(event);
    }

    @Override
    @Transactional
    public void registerForEvent(Long eventId, Long userId) {
        Event event = getEventById(eventId);
        User user = userService.getUserById(userId);

        if (event.getRegisteredUsers().contains(user)) {
            throw new ApiRequestException(ALREADY_REGISTERED, HttpStatus.BAD_REQUEST);
        }

        if (event.isFull()) {
            throw new ApiRequestException(EVENT_FULL, HttpStatus.BAD_REQUEST);
        }

        if (!event.isRegistrationOpen()) {
            throw new ApiRequestException(REGISTRATION_CLOSED, HttpStatus.BAD_REQUEST);
        }

        event.getRegisteredUsers().add(user);
        eventRepository.save(event);
    }

    @Override
    @Transactional
    public void unregisterFromEvent(Long eventId, Long userId) {
        Event event = getEventById(eventId);
        User user = userService.getUserById(userId);

        if (!event.getRegisteredUsers().contains(user)) {
            throw new ApiRequestException("Not registered for this event", HttpStatus.BAD_REQUEST);
        }

        event.getRegisteredUsers().remove(user);
        eventRepository.save(event);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserRegistered(Long eventId, Long userId) {
        return eventRepository.isUserRegistered(eventId, userId);
    }
}
