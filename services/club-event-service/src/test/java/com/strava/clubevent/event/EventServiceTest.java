package com.strava.clubevent.event;

import com.strava.clubevent.club.Club;
import com.strava.clubevent.club.ClubRepository;
import com.strava.clubevent.common.ActivityType;
import com.strava.clubevent.exception.BadRequestException;
import com.strava.clubevent.exception.ForbiddenException;
import com.strava.clubevent.exception.NotFoundException;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventService Unit Tests")
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ClubRepository clubRepository;

    @InjectMocks
    private EventService eventService;

    private Event testEvent;
    private EventRequest eventRequest;

    @BeforeEach
    void setUp() {
        testEvent = Event.builder()
                .id(1L)
                .name("Marathon 2024")
                .description("Annual city marathon")
                .activityType(ActivityType.RUN)
                .location("City Center")
                .city("Warsaw")
                .country("Poland")
                .eventDate(LocalDateTime.now().plusDays(30))
                .registrationDeadline(LocalDateTime.now().plusDays(20))
                .maxParticipants(1000)
                .isPublic(true)
                .organizerId(1L)
                .build();

        eventRequest = new EventRequest();
        eventRequest.setName("Marathon 2024");
        eventRequest.setDescription("Annual city marathon");
        eventRequest.setActivityType(ActivityType.RUN);
        eventRequest.setEventDate(LocalDateTime.now().plusDays(30));
        eventRequest.setRegistrationDeadline(LocalDateTime.now().plusDays(20));
        eventRequest.setLocation("City Center");
        eventRequest.setCity("Warsaw");
        eventRequest.setCountry("Poland");
        eventRequest.setDistance(42.195);
        eventRequest.setMaxParticipants(1000);
        eventRequest.setPublic(true);
    }

    @Nested
    @DisplayName("getEventById Tests")
    class GetEventByIdTests {

        @Test
        @DisplayName("Should return event when found")
        void shouldReturnEventWhenFound() {
            given(eventRepository.findById(1L)).willReturn(Optional.of(testEvent));

            Event result = eventService.getEventById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Marathon 2024");
        }

        @Test
        @DisplayName("Should throw exception when event not found")
        void shouldThrowExceptionWhenEventNotFound() {
            given(eventRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.getEventById(999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Event not found");
        }
    }

    @Nested
    @DisplayName("getUpcomingPublicEvents Tests")
    class GetUpcomingPublicEventsTests {

        @Test
        @DisplayName("Should return paginated upcoming public events")
        void shouldReturnUpcomingPublicEvents() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Event> eventPage = new PageImpl<>(List.of(testEvent));
            given(eventRepository.findUpcomingPublicEvents(any(LocalDateTime.class), any(Pageable.class)))
                    .willReturn(eventPage);

            Page<Event> result = eventService.getUpcomingPublicEvents(pageable);

            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getUpcomingEventsByUserId Tests")
    class GetUpcomingEventsByUserIdTests {

        @Test
        @DisplayName("Should return upcoming events for user")
        void shouldReturnUpcomingEventsForUser() {
            given(eventRepository.findUpcomingEventsByUserId(any(Long.class), any(LocalDateTime.class)))
                    .willReturn(List.of(testEvent));

            List<Event> result = eventService.getUpcomingEventsByUserId(1L);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("createEvent Tests")
    class CreateEventTests {

        @Test
        @DisplayName("Should create event successfully")
        void shouldCreateEventSuccessfully() {
            given(eventRepository.save(any(Event.class))).willAnswer(invocation -> {
                Event saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            Event result = eventService.createEvent(1L, eventRequest);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Marathon 2024");
            assertThat(result.getOrganizerId()).isEqualTo(1L);
            assertThat(result.getClubId()).isNull();
            assertThat(result.isPublic()).isTrue();
        }

        @Test
        @DisplayName("Should link event to club when clubId is set")
        void shouldLinkEventToClub() {
            eventRequest.setClubId(5L);
            given(clubRepository.findById(5L)).willReturn(Optional.of(
                    Club.builder().id(5L).name("Runners").ownerId(1L).build()));
            given(eventRepository.save(any(Event.class))).willAnswer(invocation -> invocation.getArgument(0));

            Event result = eventService.createEvent(1L, eventRequest);

            assertThat(result.getClubId()).isEqualTo(5L);
        }

        @Test
        @DisplayName("Should throw exception when club not found")
        void shouldThrowExceptionWhenClubNotFound() {
            eventRequest.setClubId(99L);
            given(clubRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.createEvent(1L, eventRequest))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Club not found");

            then(eventRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateEvent Tests")
    class UpdateEventTests {

        @Test
        @DisplayName("Should update event when user is organizer")
        void shouldUpdateEventWhenUserIsOrganizer() {
            given(eventRepository.findById(1L)).willReturn(Optional.of(testEvent));
            given(eventRepository.save(any(Event.class))).willReturn(testEvent);

            eventRequest.setName("Updated Marathon");
            Event result = eventService.updateEvent(1L, 1L, eventRequest);

            assertThat(result.getName()).isEqualTo("Updated Marathon");
            then(eventRepository).should().save(testEvent);
        }

        @Test
        @DisplayName("Should throw exception when user is not organizer")
        void shouldThrowExceptionWhenUserIsNotOrganizer() {
            given(eventRepository.findById(1L)).willReturn(Optional.of(testEvent));

            assertThatThrownBy(() -> eventService.updateEvent(1L, 2L, eventRequest))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("You don't have permission to update this event");
        }
    }

    @Nested
    @DisplayName("deleteEvent Tests")
    class DeleteEventTests {

        @Test
        @DisplayName("Should delete event when user is organizer")
        void shouldDeleteEventWhenUserIsOrganizer() {
            given(eventRepository.findById(1L)).willReturn(Optional.of(testEvent));

            eventService.deleteEvent(1L, 1L);

            then(eventRepository).should().delete(testEvent);
        }

        @Test
        @DisplayName("Should throw exception when user is not organizer")
        void shouldThrowExceptionWhenUserIsNotOrganizer() {
            given(eventRepository.findById(1L)).willReturn(Optional.of(testEvent));

            assertThatThrownBy(() -> eventService.deleteEvent(1L, 2L))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("You don't have permission to delete this event");

            then(eventRepository).should(never()).delete(any());
        }
    }

    @Nested
    @DisplayName("registerForEvent Tests")
    class RegisterForEventTests {

        @Test
        @DisplayName("Should register for event successfully")
        void shouldRegisterForEventSuccessfully() {
            given(eventRepository.findById(1L)).willReturn(Optional.of(testEvent));
            given(eventRepository.save(any(Event.class))).willReturn(testEvent);

            eventService.registerForEvent(1L, 2L);

            assertThat(testEvent.getRegisteredUserIds()).contains(2L);
            then(eventRepository).should().save(testEvent);
        }

        @Test
        @DisplayName("Should throw exception when already registered")
        void shouldThrowExceptionWhenAlreadyRegistered() {
            testEvent.getRegisteredUserIds().add(2L);
            given(eventRepository.findById(1L)).willReturn(Optional.of(testEvent));

            assertThatThrownBy(() -> eventService.registerForEvent(1L, 2L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("You are already registered for this event");
        }

        @Test
        @DisplayName("Should throw exception when event is full")
        void shouldThrowExceptionWhenEventIsFull() {
            testEvent.setMaxParticipants(1);
            testEvent.getRegisteredUserIds().add(3L);
            given(eventRepository.findById(1L)).willReturn(Optional.of(testEvent));

            assertThatThrownBy(() -> eventService.registerForEvent(1L, 2L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("This event is full");
        }

        @Test
        @DisplayName("Should throw exception when registration deadline has passed")
        void shouldThrowExceptionWhenRegistrationClosed() {
            testEvent.setRegistrationDeadline(LocalDateTime.now().minusDays(1));
            given(eventRepository.findById(1L)).willReturn(Optional.of(testEvent));

            assertThatThrownBy(() -> eventService.registerForEvent(1L, 2L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Registration for this event is closed");
        }
    }

    @Nested
    @DisplayName("unregisterFromEvent Tests")
    class UnregisterFromEventTests {

        @Test
        @DisplayName("Should unregister from event successfully")
        void shouldUnregisterFromEventSuccessfully() {
            testEvent.getRegisteredUserIds().add(2L);
            given(eventRepository.findById(1L)).willReturn(Optional.of(testEvent));
            given(eventRepository.save(any(Event.class))).willReturn(testEvent);

            eventService.unregisterFromEvent(1L, 2L);

            assertThat(testEvent.getRegisteredUserIds()).doesNotContain(2L);
            then(eventRepository).should().save(testEvent);
        }

        @Test
        @DisplayName("Should throw exception when not registered")
        void shouldThrowExceptionWhenNotRegistered() {
            given(eventRepository.findById(1L)).willReturn(Optional.of(testEvent));

            assertThatThrownBy(() -> eventService.unregisterFromEvent(1L, 2L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Not registered for this event");
        }
    }

    @Nested
    @DisplayName("isUserRegistered Tests")
    class IsUserRegisteredTests {

        @Test
        @DisplayName("Should delegate to repository")
        void shouldDelegateToRepository() {
            given(eventRepository.isUserRegistered(1L, 2L)).willReturn(true);

            assertThat(eventService.isUserRegistered(1L, 2L)).isTrue();
        }
    }
}
