package com.strava.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.strava.backend.domain.Goal;
import com.strava.backend.domain.User;
import com.strava.backend.dto.goal.GoalRequest;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.enums.GoalPeriod;
import com.strava.backend.enums.GoalType;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.security.CustomUserDetailsService;
import com.strava.backend.security.jwt.JwtProvider;
import com.strava.backend.service.GoalService;
import com.strava.backend.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GoalController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("GoalController Unit Tests")
class GoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoalService goalService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private ObjectMapper objectMapper;
    private Goal testGoal;
    private User testUser;
    private GoalRequest goalRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        testUser = TestDataFactory.createUser();
        testGoal = TestDataFactory.createGoal(1L, testUser);
        goalRequest = TestDataFactory.createGoalRequest();
    }

    @Nested
    @DisplayName("GET /api/v1/goals")
    class GetActiveGoalsTests {

        @Test
        @DisplayName("Should return active goals")
        void shouldReturnActiveGoals() throws Exception {
            given(goalService.getActiveGoalsByUserId(1L)).willReturn(List.of(testGoal));

            mockMvc.perform(get("/api/v1/goals")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].goalType").value("DISTANCE"))
                    .andExpect(jsonPath("$[0].period").value("WEEKLY"));
        }

        @Test
        @DisplayName("Should return empty list when no active goals")
        void shouldReturnEmptyListWhenNoActiveGoals() throws Exception {
            given(goalService.getActiveGoalsByUserId(1L)).willReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/goals")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("Should use default user ID when not provided")
        void shouldUseDefaultUserIdWhenNotProvided() throws Exception {
            given(goalService.getActiveGoalsByUserId(1L)).willReturn(List.of(testGoal));

            mockMvc.perform(get("/api/v1/goals"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/goals/all")
    class GetAllGoalsTests {

        @Test
        @DisplayName("Should return all goals")
        void shouldReturnAllGoals() throws Exception {
            Goal inactiveGoal = TestDataFactory.createGoal(2L, testUser);
            inactiveGoal.setActive(false);
            given(goalService.getAllGoalsByUserId(1L)).willReturn(List.of(testGoal, inactiveGoal));

            mockMvc.perform(get("/api/v1/goals/all")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/goals/{goalId}")
    class GetGoalByIdTests {

        @Test
        @DisplayName("Should return goal when found")
        void shouldReturnGoalWhenFound() throws Exception {
            given(goalService.getGoalById(1L)).willReturn(testGoal);

            mockMvc.perform(get("/api/v1/goals/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.goalType").value("DISTANCE"))
                    .andExpect(jsonPath("$.targetValue").value(50.0))
                    .andExpect(jsonPath("$.currentValue").value(25.0));
        }

        @Test
        @DisplayName("Should return 404 when goal not found")
        void shouldReturn404WhenGoalNotFound() throws Exception {
            given(goalService.getGoalById(99L))
                    .willThrow(new ApiRequestException("Goal not found", HttpStatus.NOT_FOUND));

            mockMvc.perform(get("/api/v1/goals/99"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/goals")
    class CreateGoalTests {

        @Test
        @DisplayName("Should create goal successfully")
        void shouldCreateGoalSuccessfully() throws Exception {
            given(goalService.createGoal(eq(1L), any(GoalRequest.class))).willReturn(testGoal);

            mockMvc.perform(post("/api/v1/goals")
                            .param("userId", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(goalRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.goalType").value("DISTANCE"));
        }

        @Test
        @DisplayName("Should return bad request when goal type is null")
        void shouldReturnBadRequestWhenGoalTypeIsNull() throws Exception {
            goalRequest.setGoalType(null);

            mockMvc.perform(post("/api/v1/goals")
                            .param("userId", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(goalRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return bad request when period is null")
        void shouldReturnBadRequestWhenPeriodIsNull() throws Exception {
            goalRequest.setPeriod(null);

            mockMvc.perform(post("/api/v1/goals")
                            .param("userId", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(goalRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return bad request when target value is null")
        void shouldReturnBadRequestWhenTargetValueIsNull() throws Exception {
            goalRequest.setTargetValue(null);

            mockMvc.perform(post("/api/v1/goals")
                            .param("userId", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(goalRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return bad request when target value is negative")
        void shouldReturnBadRequestWhenTargetValueIsNegative() throws Exception {
            goalRequest.setTargetValue(-10.0);

            mockMvc.perform(post("/api/v1/goals")
                            .param("userId", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(goalRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/goals/{goalId}")
    class UpdateGoalTests {

        @Test
        @DisplayName("Should update goal successfully")
        void shouldUpdateGoalSuccessfully() throws Exception {
            testGoal.setTargetValue(150.0);
            given(goalService.updateGoal(eq(1L), eq(1L), any(GoalRequest.class))).willReturn(testGoal);

            mockMvc.perform(put("/api/v1/goals/1")
                            .param("userId", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(goalRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.targetValue").value(150.0));
        }

        @Test
        @DisplayName("Should return 403 when not goal owner")
        void shouldReturn403WhenNotGoalOwner() throws Exception {
            given(goalService.updateGoal(eq(1L), eq(2L), any(GoalRequest.class)))
                    .willThrow(new ApiRequestException("Not authorized", HttpStatus.FORBIDDEN));

            mockMvc.perform(put("/api/v1/goals/1")
                            .param("userId", "2")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(goalRequest)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/goals/{goalId}")
    class DeleteGoalTests {

        @Test
        @DisplayName("Should delete goal successfully")
        void shouldDeleteGoalSuccessfully() throws Exception {
            willDoNothing().given(goalService).deleteGoal(1L, 1L);

            mockMvc.perform(delete("/api/v1/goals/1")
                            .param("userId", "1"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/goals/progress")
    class UpdateGoalProgressTests {

        @Test
        @DisplayName("Should update goal progress successfully")
        void shouldUpdateGoalProgressSuccessfully() throws Exception {
            willDoNothing().given(goalService).updateGoalProgress(1L);

            mockMvc.perform(post("/api/v1/goals/progress")
                            .param("userId", "1"))
                    .andExpect(status().isOk());
        }
    }
}
