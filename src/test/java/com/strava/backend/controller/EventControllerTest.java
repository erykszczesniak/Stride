package com.strava.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.strava.backend.domain.Event;
import com.strava.backend.domain.User;
import com.strava.backend.dto.event.EventRequest;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.security.CustomUserDetailsService;
import com.strava.backend.security.UserPrincipal;
import com.strava.backend.security.jwt.JwtProvider;
import com.strava.backend.service.EventService;
import com.strava.backend.util.TestDataFactory;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
@ActiveProfiles("test")
@DisplayName("EventController Unit Tests")
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private ObjectMapper objectMapper;
    private Event testEvent;
    private User testUser;
    private EventRequest eventRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        testUser = TestDataFactory.createUser();
        testEvent = TestDataFactory.createEvent(1L, testUser);
        eventRequest = TestDataFactory.createEventRequest();

        UserPrincipal userPrincipal = UserPrincipal.builder()
                .id(1L)
                .email("test@example.com")
                .password("password")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
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
                    .andExpect(jsonPath("$.isRegistered").value(true));
        }

        @Test
        @DisplayName("Should return 404 when event not found")
        void shouldReturn404WhenEventNotFound() throws Exception {
            given(eventService.getEventById(99L))
                    .willThrow(new ApiRequestException("Event not found", HttpStatus.NOT_FOUND));

            mockMvc.perform(get("/api/v1/events/99"))
                    .andExpect(status().isNotFound());
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
                    .willThrow(new ApiRequestException("Not authorized", HttpStatus.FORBIDDEN));

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
            willThrow(new ApiRequestException("Event is full", HttpStatus.BAD_REQUEST))
                    .given(eventService).registerForEvent(1L, 1L);

            mockMvc.perform(post("/api/v1/events/1/register"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return error when already registered")
        void shouldReturnErrorWhenAlreadyRegistered() throws Exception {
            willThrow(new ApiRequestException("Already registered", HttpStatus.BAD_REQUEST))
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
