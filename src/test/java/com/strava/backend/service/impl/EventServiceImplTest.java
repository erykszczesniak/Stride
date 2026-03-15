package com.strava.backend.service.impl;

import com.strava.backend.domain.Club;
import com.strava.backend.domain.Event;
import com.strava.backend.domain.User;
import com.strava.backend.dto.event.EventRequest;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.repository.ClubRepository;
import com.strava.backend.repository.EventRepository;
import com.strava.backend.service.UserService;
import com.strava.backend.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static com.strava.backend.constants.ErrorMessage.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventServiceImpl Unit Tests")
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private EventServiceImpl eventService;

    private User organizer;
    private User participant;
    private Event testEvent;
    private Club testClub;
    private EventRequest eventRequest;

    @BeforeEach
    void setUp() {
        organizer = TestDataFactory.createUser(1L, "organizer@example.com", "Organizer", "User");
        participant = TestDataFactory.createUser(2L, "participant@example.com", "Participant", "User");
        testEvent = TestDataFactory.createEvent(1L, organizer);
        testClub = TestDataFactory.createClub(1L, organizer);
        eventRequest = TestDataFactory.createEventRequest();
    }

    @Nested
    @DisplayName("getEventById Tests")
    class GetEventByIdTests {

        @Test
        @DisplayName("Should return event when found")
        void shouldReturnEventWhenFound() {
            // Given
            given(eventRepository.findById(1L)).willReturn(Optional.of(testEvent));

            // When
            Event result = eventService.getEventById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Marathon 2024");
        }

        @Test
        @DisplayName("Should throw exception when event not found")
        void shouldThrowExceptionWhenEventNotFound() {
            // Given
            given(eventRepository.findById(999L)).willReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> eventService.getEventById(999L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(EVENT_NOT_FOUND)
                    .extracting("status")
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getUpcomingPublicEvents Tests")
    class GetUpcomingPublicEventsTests {

        @Test
        @DisplayName("Should return paginated upcoming events")
        void shouldReturnPaginatedUpcomingEvents() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Event> eventPage = new PageImpl<>(List.of(testEvent));
            given(eventRepository.findUpcomingPublicEvents(any(LocalDateTime.class), eq(pageable)))
                    .willReturn(eventPage);

            // When
            Page<Event> result = eventService.getUpcomingPublicEvents(pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getUpcomingEventsByUserId Tests")
    class GetUpcomingEventsByUserIdTests {

        @Test
        @DisplayName("Should return upcoming events for user")
        void shouldReturnUpcomingEventsForUser() {
            // Given
            given(eventRepository.findUpcomingEventsByUserId(eq(1L), any(LocalDateTime.class)))
                    .willReturn(List.of(testEvent));

            // When
            List<Event> result = eventService.getUpcomingEventsByUserId(1L);

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should return limited upcoming events for user")
        void shouldReturnLimitedUpcomingEventsForUser() {
            // Given
            Event event2 = TestDataFactory.createEvent(2L, organizer);
            given(eventRepository.findUpcomingEventsByUserId(eq(1L), any(LocalDateTime.class), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(testEvent, event2)));

            // When
            List<Event> result = eventService.getUpcomingEventsByUserId(1L, 2);

            // Then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("createEvent Tests")
    class CreateEventTests {

        @Test
        @DisplayName("Should create event successfully without club")
        void shouldCreateEventSuccessfullyWithoutClub() {
            // Given
            eventRequest.setClubId(null);
            given(userService.getUserById(1L)).willReturn(organizer);
            given(eventRepository.save(any(Event.class))).willAnswer(invocation -> {
                Event saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            // When
            Event result = eventService.createEvent(1L, eventRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Marathon 2024");
            assertThat(result.getOrganizer()).isEqualTo(organizer);
            assertThat(result.getClub()).isNull();

            then(eventRepository).should().save(any(Event.class));
        }

        @Test
        @DisplayName("Should create event successfully with club")
        void shouldCreateEventSuccessfullyWithClub() {
            // Given
            eventRequest.setClubId(1L);
            given(userService.getUserById(1L)).willReturn(organizer);
            given(clubRepository.findById(1L)).willReturn(Optional.of(testClub));
            given(eventRepository.save(any(Event.class))).willAnswer(invocation -> {
                Event saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            // When
            Event result = eventService.createEvent(1L, eventRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getClub()).isEqualTo(testClub);
        }

        @Test
        @DisplayName("Should throw exception when club not found")
        void shouldThrowExceptionWhenClubNotFound() {
            // Given
            eventRequest.setClubId(999L);
            given(userService.getUserById(1L)).willReturn(organizer);
            given(clubRepository.findById(999L)).willReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> eventService.createEvent(1L, eventRequest))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(CLUB_NOT_FOUND);

            then(eventRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateEvent Tests")
    class UpdateEventTests {

        @Test
        @DisplayName("Should update event when user is organizer")
        void shouldUpdateEventWhenUserIsOrganizer() {
            // Given
            given(eventRepository.findById(1L)).willReturn(Optional.of(testEvent));
            given(eventRepository.save(any(Event.class))).willReturn(testEvent);

            // When
            Event result = eventService.updateEvent(1L, 1L, eventRequest);

            // Then
            assertThat(result.getName()).isEqualTo("Marathon 2024");
            then(eventRepository).should().save(testEvent);
        }

        @Test
        @DisplayName("Should throw exception when user is not organizer")
        void shouldThrowExceptionWhenUserIsNotOrganizer() {
            // Given
            given(eventRepository.findById(1L)).willReturn(Optional.of(testEvent));

            // When/Then
            assertThatThrownBy(() -> eventService.updateEvent(1L, 2L, eventRequest))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("You don't have permission to update this event")
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);

            then(eventRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteEvent Tests")
    class DeleteEventTests {

        @Test
        @DisplayName("Should delete event when user is organizer")
        void shouldDeleteEventWhenUserIsOrganizer() {
            // Given
            given(eventRepository.findById(1L)).willReturn(Optional.of(testEvent));

            // When
            eventService.deleteEvent(1L, 1L);

            // Then
            then(eventRepository).should().delete(testEvent);
        }

        @Test
        @DisplayName("Should throw exception when user is not organizer")
        void shouldThrowExceptionWhenUserIsNotOrganizer() {
            // Given
            given(eventRepository.findById(1L)).willReturn(Optional.of(testEvent));

            // When/Then
            assertThatThrownBy(() -> eventService.deleteEvent(1L, 2L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("You don't have permission to delete this event")
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);

            then(eventRepository).should(never()).delete(any());
        }
    }

    @Nested
    @DisplayName("registerForEvent Tests")
    class RegisterForEventTests {

        @Test
        @DisplayName("Should register for event successfully")
        void shouldRegisterForEventSuccessfully() {
            // Given
            testEvent.setMaxParticipants(100);
            testEvent.setRegistrationDeadline(LocalDateTime.now().plusDays(10));
            testEvent.setRegisteredUsers(new HashSet<>());

            given(eventRepository.findById(1L)).willReturn(Optional.of(testEvent));
            given(userService.getUserById(2L)).willReturn(participant);
            given(eventRepository.save(any(Event.class))).willReturn(testEvent);

            // When
            eventService.registerForEvent(1L, 2L);

            // Then
            assertThat(testEvent.getRegisteredUsers()).contains(participant);
            then(eventRepository).should().save(testEvent);
        }

        @Test
        @DisplayName("Should throw exception when already registered")
        void shouldThrowExceptionWhenAlreadyRegistered() {
            // Given
            testEvent.getRegisteredUsers().add(participant);
            given(eventRepository.findById(1L)).willReturn(Optional.of(testEvent));
            given(userService.getUserById(2L)).willReturn(participant);

            // When/Then
            assertThatThrownBy(() -> eventService.registerForEvent(1L, 2L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(ALREADY_REGISTERED)
                    .extracting("status")
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should throw exception when event is full")
        void shouldThrowExceptionWhenEventIsFull() {
            // Given
            testEvent.setMaxParticipants(1);
            testEvent.getRegisteredUsers().add(organizer); // Already 1 participant
            testEvent.setRegistrationDeadline(LocalDateTime.now().plusDays(10));

            given(eventRepository.findById(1L)).willReturn(Optional.of(testEvent));
            given(userService.getUserById(2L)).willReturn(participant);

            // When/Then
            assertThatThrownBy(() -> eventService.registerForEvent(1L, 2L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(EVENT_FULL)
                    .extracting("status")
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should throw exception when registration is closed")
        void shouldThrowExceptionWhenRegistrationClosed() {
            // Given
            testEvent.setRegistrationDeadline(LocalDateTime.now().minusDays(1)); // Deadline passed
            testEvent.setMaxParticipants(100);
            testEvent.setRegisteredUsers(new HashSet<>());

            given(eventRepository.findById(1L)).willReturn(Optional.of(testEvent));
            given(userService.getUserById(2L)).willReturn(participant);

            // When/Then
            assertThatThrownBy(() -> eventService.registerForEvent(1L, 2L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(REGISTRATION_CLOSED)
                    .extracting("status")
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("unregisterFromEvent Tests")
    class UnregisterFromEventTests {

        @Test
        @DisplayName("Should unregister from event successfully")
        void shouldUnregisterFromEventSuccessfully() {
            // Given
            testEvent.getRegisteredUsers().add(participant);
            given(eventRepository.findById(1L)).willReturn(Optional.of(testEvent));
            given(userService.getUserById(2L)).willReturn(participant);
            given(eventRepository.save(any(Event.class))).willReturn(testEvent);

            // When
            eventService.unregisterFromEvent(1L, 2L);

            // Then
            assertThat(testEvent.getRegisteredUsers()).doesNotContain(participant);
            then(eventRepository).should().save(testEvent);
        }

        @Test
        @DisplayName("Should throw exception when not registered")
        void shouldThrowExceptionWhenNotRegistered() {
            // Given
            testEvent.setRegisteredUsers(new HashSet<>());
            given(eventRepository.findById(1L)).willReturn(Optional.of(testEvent));
            given(userService.getUserById(2L)).willReturn(participant);

            // When/Then
            assertThatThrownBy(() -> eventService.unregisterFromEvent(1L, 2L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("Not registered for this event")
                    .extracting("status")
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("isUserRegistered Tests")
    class IsUserRegisteredTests {

        @Test
        @DisplayName("Should return true when user is registered")
        void shouldReturnTrueWhenUserIsRegistered() {
            // Given
            given(eventRepository.isUserRegistered(1L, 2L)).willReturn(true);

            // When
            boolean result = eventService.isUserRegistered(1L, 2L);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when user is not registered")
        void shouldReturnFalseWhenUserIsNotRegistered() {
            // Given
            given(eventRepository.isUserRegistered(1L, 3L)).willReturn(false);

            // When
            boolean result = eventService.isUserRegistered(1L, 3L);

            // Then
            assertThat(result).isFalse();
        }
    }
}
