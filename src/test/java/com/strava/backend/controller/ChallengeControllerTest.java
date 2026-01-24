package com.strava.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.strava.backend.domain.Challenge;
import com.strava.backend.domain.User;
import com.strava.backend.dto.challenge.ChallengeRequest;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.enums.ChallengeType;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.security.CustomUserDetailsService;
import com.strava.backend.security.jwt.JwtProvider;
import com.strava.backend.service.ChallengeService;
import com.strava.backend.util.TestDataFactory;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChallengeController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ChallengeController Unit Tests")
class ChallengeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChallengeService challengeService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private ObjectMapper objectMapper;
    private Challenge testChallenge;
    private User testUser;
    private ChallengeRequest challengeRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        testUser = TestDataFactory.createUser();
        testChallenge = TestDataFactory.createChallenge(1L, testUser);

        challengeRequest = new ChallengeRequest();
        challengeRequest.setName("Monthly Running Challenge");
        challengeRequest.setDescription("Run 100km this month");
        challengeRequest.setChallengeType(ChallengeType.DISTANCE);
        challengeRequest.setActivityType(ActivityType.RUN);
        challengeRequest.setTargetValue(100.0);
        challengeRequest.setStartDate(LocalDateTime.now());
        challengeRequest.setEndDate(LocalDateTime.now().plusDays(30));
        challengeRequest.setPublic(true);
    }

    @Nested
    @DisplayName("GET /api/v1/challenges")
    class GetActiveChallengesTests {

        @Test
        @DisplayName("Should return active challenges")
        void shouldReturnActiveChallenges() throws Exception {
            Page<Challenge> challengePage = new PageImpl<>(List.of(testChallenge));
            given(challengeService.getActiveChallenges(any(Pageable.class))).willReturn(challengePage);
            given(challengeService.isUserParticipating(1L, 1L)).willReturn(false);

            mockMvc.perform(get("/api/v1/challenges")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].name").value("Monthly Running Challenge"));
        }

        @Test
        @DisplayName("Should return empty list when no active challenges")
        void shouldReturnEmptyListWhenNoActiveChallenges() throws Exception {
            Page<Challenge> emptyPage = new PageImpl<>(Collections.emptyList());
            given(challengeService.getActiveChallenges(any(Pageable.class))).willReturn(emptyPage);

            mockMvc.perform(get("/api/v1/challenges"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/challenges/upcoming")
    class GetUpcomingChallengesTests {

        @Test
        @DisplayName("Should return upcoming challenges")
        void shouldReturnUpcomingChallenges() throws Exception {
            Page<Challenge> challengePage = new PageImpl<>(List.of(testChallenge));
            given(challengeService.getUpcomingChallenges(any(Pageable.class))).willReturn(challengePage);
            given(challengeService.isUserParticipating(1L, 1L)).willReturn(false);

            mockMvc.perform(get("/api/v1/challenges/upcoming")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/challenges/{challengeId}")
    class GetChallengeByIdTests {

        @Test
        @DisplayName("Should return challenge when found")
        void shouldReturnChallengeWhenFound() throws Exception {
            given(challengeService.getChallengeById(1L)).willReturn(testChallenge);
            given(challengeService.isUserParticipating(1L, 1L)).willReturn(true);
            given(challengeService.getUserProgressInChallenge(1L, 1L)).willReturn(50.0);

            mockMvc.perform(get("/api/v1/challenges/1")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Monthly Running Challenge"))
                    .andExpect(jsonPath("$.participating").value(true))
                    .andExpect(jsonPath("$.userProgress").value(50.0));
        }

        @Test
        @DisplayName("Should return 404 when challenge not found")
        void shouldReturn404WhenChallengeNotFound() throws Exception {
            given(challengeService.getChallengeById(99L))
                    .willThrow(new ApiRequestException("Challenge not found", HttpStatus.NOT_FOUND));

            mockMvc.perform(get("/api/v1/challenges/99"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/challenges/my")
    class GetMyChallengesTests {

        @Test
        @DisplayName("Should return user's challenges")
        void shouldReturnUserChallenges() throws Exception {
            given(challengeService.getUserActiveChallenges(1L)).willReturn(List.of(testChallenge));
            given(challengeService.isUserParticipating(1L, 1L)).willReturn(true);
            given(challengeService.getUserProgressInChallenge(1L, 1L)).willReturn(25.0);

            mockMvc.perform(get("/api/v1/challenges/my")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].participating").value(true));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/challenges")
    class CreateChallengeTests {

        @Test
        @DisplayName("Should create challenge successfully")
        void shouldCreateChallengeSuccessfully() throws Exception {
            given(challengeService.createChallenge(any(Challenge.class), eq(1L))).willReturn(testChallenge);
            given(challengeService.isUserParticipating(1L, 1L)).willReturn(false);

            mockMvc.perform(post("/api/v1/challenges")
                            .param("userId", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(challengeRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Monthly Running Challenge"));
        }

        @Test
        @DisplayName("Should return bad request when name is blank")
        void shouldReturnBadRequestWhenNameIsBlank() throws Exception {
            challengeRequest.setName("");

            mockMvc.perform(post("/api/v1/challenges")
                            .param("userId", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(challengeRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return bad request when target value is null")
        void shouldReturnBadRequestWhenTargetValueIsNull() throws Exception {
            challengeRequest.setTargetValue(null);

            mockMvc.perform(post("/api/v1/challenges")
                            .param("userId", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(challengeRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/challenges/{challengeId}")
    class UpdateChallengeTests {

        @Test
        @DisplayName("Should update challenge successfully")
        void shouldUpdateChallengeSuccessfully() throws Exception {
            testChallenge.setName("Updated Challenge");
            given(challengeService.updateChallenge(eq(1L), any(Challenge.class), eq(1L)))
                    .willReturn(testChallenge);
            given(challengeService.isUserParticipating(1L, 1L)).willReturn(false);

            mockMvc.perform(put("/api/v1/challenges/1")
                            .param("userId", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(challengeRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Challenge"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/challenges/{challengeId}")
    class DeleteChallengeTests {

        @Test
        @DisplayName("Should delete challenge successfully")
        void shouldDeleteChallengeSuccessfully() throws Exception {
            willDoNothing().given(challengeService).deleteChallenge(1L, 1L);

            mockMvc.perform(delete("/api/v1/challenges/1")
                            .param("userId", "1"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/challenges/{challengeId}/participate")
    class JoinChallengeTests {

        @Test
        @DisplayName("Should join challenge successfully")
        void shouldJoinChallengeSuccessfully() throws Exception {
            willDoNothing().given(challengeService).joinChallenge(1L, 1L);

            mockMvc.perform(post("/api/v1/challenges/1/participate")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Successfully joined the challenge"));
        }

        @Test
        @DisplayName("Should return error when already participating")
        void shouldReturnErrorWhenAlreadyParticipating() throws Exception {
            willThrow(new ApiRequestException("Already participating", HttpStatus.BAD_REQUEST))
                    .given(challengeService).joinChallenge(1L, 1L);

            mockMvc.perform(post("/api/v1/challenges/1/participate")
                            .param("userId", "1"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/challenges/{challengeId}/participate")
    class LeaveChallengeTests {

        @Test
        @DisplayName("Should leave challenge successfully")
        void shouldLeaveChallengeSuccessfully() throws Exception {
            willDoNothing().given(challengeService).leaveChallenge(1L, 1L);

            mockMvc.perform(delete("/api/v1/challenges/1/participate")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Successfully left the challenge"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/challenges/{challengeId}/leaderboard")
    class GetChallengeLeaderboardTests {

        @Test
        @DisplayName("Should return challenge leaderboard")
        void shouldReturnChallengeLeaderboard() throws Exception {
            testChallenge.setParticipants(new HashSet<>(List.of(testUser)));
            given(challengeService.getChallengeById(1L)).willReturn(testChallenge);
            given(challengeService.getUserProgressInChallenge(1L, 1L)).willReturn(75.0);

            mockMvc.perform(get("/api/v1/challenges/1/leaderboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].userId").value(1))
                    .andExpect(jsonPath("$[0].progress").value(75.0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/challenges/search")
    class SearchChallengesTests {

        @Test
        @DisplayName("Should search challenges by query")
        void shouldSearchChallengesByQuery() throws Exception {
            Page<Challenge> challengePage = new PageImpl<>(List.of(testChallenge));
            given(challengeService.searchChallenges(eq("Running"), any(Pageable.class)))
                    .willReturn(challengePage);
            given(challengeService.isUserParticipating(1L, 1L)).willReturn(false);

            mockMvc.perform(get("/api/v1/challenges/search")
                            .param("q", "Running")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("Monthly Running Challenge"));
        }
    }
}
