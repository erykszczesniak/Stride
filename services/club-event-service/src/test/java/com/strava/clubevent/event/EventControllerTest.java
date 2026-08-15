package com.strava.clubevent.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.strava.clubevent.common.ActivityType;
import com.strava.clubevent.exception.BadRequestException;
import com.strava.clubevent.exception.ForbiddenException;
import com.strava.clubevent.exception.NotFoundException;
import com.strava.shared.security.HeaderAuthenticationFilter.HeaderAuthenticationToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("EventController Unit Tests")
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    private ObjectMapper objectMapper;
    private Event testEvent;
    private EventRequest eventRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

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

        SecurityContextHolder.getContext().setAuthentication(new HeaderAuthenticationToken(
                1L, "test@example.com", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("GET /api/v1/events")
    class GetUpcomingEventsTests {

        @Test
        @DisplayName("Should return upcoming events")
        void shouldReturnUpcomingEvents() throws Exception {
            Page<Event> eventPage = new PageImpl<>(List.of(testEvent));
            given(eventService.getUpcomingPublicEvents(any(Pageable.class))).willReturn(eventPage);
            given(eventService.isUserRegistered(1L, 1L)).willReturn(false);

            mockMvc.perform(get("/api/v1/events"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].name").value("Marathon 2024"));
        }

        @Test
        @DisplayName("Should return empty list when no events")
        void shouldReturnEmptyListWhenNoEvents() throws Exception {
            Page<Event> emptyPage = new PageImpl<>(Collections.emptyList());
            given(eventService.getUpcomingPublicEvents(any(Pageable.class))).willReturn(emptyPage);

            mockMvc.perform(get("/api/v1/events"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/events/upcoming")
    class GetUpcomingEndpointTests {

        @Test
        @DisplayName("Should return upcoming events preview")
        void shouldReturnUpcomingEventsPreview() throws Exception {
            Page<Event> eventPage = new PageImpl<>(List.of(testEvent));
            given(eventService.getUpcomingPublicEvents(any(Pageable.class))).willReturn(eventPage);
            given(eventService.isUserRegistered(1L, 1L)).willReturn(false);

            mockMvc.perform(get("/api/v1/events/upcoming"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/events/{eventId}")
    class GetEventByIdTests {

        @Test
        @DisplayName("Should return event when found")
        void shouldReturnEventWhenFound() throws Exception {
            given(eventService.getEventById(1L)).willReturn(testEvent);
            given(eventService.isUserRegistered(1L, 1L)).willReturn(true);

            mockMvc.perform(get("/api/v1/events/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Marathon 2024"))
                    .andExpect(jsonPath("$.isRegistered").value(true))
                    .andExpect(jsonPath("$.isPublic").value(true));
        }

        @Test
        @DisplayName("Should return 404 with message property when event not found")
        void shouldReturn404WhenEventNotFound() throws Exception {
            given(eventService.getEventById(99L))
                    .willThrow(new NotFoundException("Event not found"));

            mockMvc.perform(get("/api/v1/events/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Event not found"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/events/my")
    class GetMyUpcomingEventsTests {

        @Test
        @DisplayName("Should return user's upcoming events")
        void shouldReturnUserUpcomingEvents() throws Exception {
            given(eventService.getUpcomingEventsByUserId(1L)).willReturn(List.of(testEvent));
            given(eventService.isUserRegistered(1L, 1L)).willReturn(true);

            mockMvc.perform(get("/api/v1/events/my"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].isRegistered").value(true));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/events")
    class CreateEventTests {

        @Test
        @DisplayName("Should create event successfully")
        void shouldCreateEventSuccessfully() throws Exception {
            given(eventService.createEvent(eq(1L), any(EventRequest.class))).willReturn(testEvent);
            given(eventService.isUserRegistered(1L, 1L)).willReturn(false);

            mockMvc.perform(post("/api/v1/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(eventRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Marathon 2024"));
        }

        @Test
        @DisplayName("Should return bad request when name is blank")
        void shouldReturnBadRequestWhenNameIsBlank() throws Exception {
            eventRequest.setName("");

            mockMvc.perform(post("/api/v1/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(eventRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/events/{eventId}")
    class UpdateEventTests {

        @Test
        @DisplayName("Should update event successfully")
        void shouldUpdateEventSuccessfully() throws Exception {
            testEvent.setName("Updated Marathon");
            given(eventService.updateEvent(eq(1L), eq(1L), any(EventRequest.class))).willReturn(testEvent);
            given(eventService.isUserRegistered(1L, 1L)).willReturn(false);

            mockMvc.perform(put("/api/v1/events/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(eventRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Marathon"));
        }

        @Test
        @DisplayName("Should return 403 when not organizer")
        void shouldReturn403WhenNotOrganizer() throws Exception {
            given(eventService.updateEvent(eq(1L), eq(1L), any(EventRequest.class)))
                    .willThrow(new ForbiddenException("Not authorized"));

            mockMvc.perform(put("/api/v1/events/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(eventRequest)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/events/{eventId}")
    class DeleteEventTests {

        @Test
        @DisplayName("Should delete event successfully")
        void shouldDeleteEventSuccessfully() throws Exception {
            willDoNothing().given(eventService).deleteEvent(1L, 1L);

            mockMvc.perform(delete("/api/v1/events/1"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/events/{eventId}/register")
    class RegisterForEventTests {

        @Test
        @DisplayName("Should register for event successfully")
        void shouldRegisterForEventSuccessfully() throws Exception {
            willDoNothing().given(eventService).registerForEvent(1L, 1L);

            mockMvc.perform(post("/api/v1/events/1/register"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Successfully registered for the event"));
        }

        @Test
        @DisplayName("Should return error when event is full")
        void shouldReturnErrorWhenEventIsFull() throws Exception {
            willThrow(new BadRequestException("This event is full"))
                    .given(eventService).registerForEvent(1L, 1L);

            mockMvc.perform(post("/api/v1/events/1/register"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("This event is full"));
        }

        @Test
        @DisplayName("Should return error when already registered")
        void shouldReturnErrorWhenAlreadyRegistered() throws Exception {
            willThrow(new BadRequestException("You are already registered for this event"))
                    .given(eventService).registerForEvent(1L, 1L);

            mockMvc.perform(post("/api/v1/events/1/register"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/events/{eventId}/register")
    class UnregisterFromEventTests {

        @Test
        @DisplayName("Should unregister from event successfully")
        void shouldUnregisterFromEventSuccessfully() throws Exception {
            willDoNothing().given(eventService).unregisterFromEvent(1L, 1L);

            mockMvc.perform(delete("/api/v1/events/1/register"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Successfully unregistered from the event"));
        }
    }
}
