package com.strava.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.backend.service.GroqService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AIController")
class AIControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GroqService groqService;

    @Nested
    @DisplayName("POST /api/v1/ai/generate-goal")
    class GenerateGoal {

        @Test
        @WithMockUser
        @DisplayName("should generate goal with valid request")
        void shouldGenerateGoalWithValidRequest() throws Exception {
            // Given
            String expectedResponse = """
                {
                    "smartGoal": "Run 5K in under 25 minutes within 3 months",
                    "analysis": "This is an achievable goal for beginners",
                    "milestones": ["Complete 2K run", "Complete 3K run", "Complete 5K run"],
                    "tips": ["Start slow", "Be consistent", "Rest adequately"]
                }
                """;

            when(groqService.generateGoal(anyString(), anyString(), anyString()))
                    .thenReturn(expectedResponse);

            Map<String, String> request = Map.of(
                    "goalType", "distance",
                    "timeframe", "3months",
                    "description", "I want to run 5K"
            );

            // When & Then
            mockMvc.perform(post("/api/v1/ai/generate-goal")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.smartGoal").exists())
                    .andExpect(jsonPath("$.analysis").exists())
                    .andExpect(jsonPath("$.milestones").isArray())
                    .andExpect(jsonPath("$.tips").isArray());
        }

        @Test
        @WithMockUser
        @DisplayName("should use default values when request parameters are missing")
        void shouldUseDefaultValuesWhenParametersMissing() throws Exception {
            // Given
            String expectedResponse = """
                {
                    "smartGoal": "Default goal",
                    "analysis": "Default analysis",
                    "milestones": [],
                    "tips": []
                }
                """;

            when(groqService.generateGoal(eq("distance"), eq("month"), eq("")))
                    .thenReturn(expectedResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/ai/generate-goal")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 401 when user is not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // Given
            Map<String, String> request = Map.of(
                    "goalType", "distance",
                    "timeframe", "month",
                    "description", "test"
            );

            // When & Then
            mockMvc.perform(post("/api/v1/ai/generate-goal")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/ai/generate-plan")
    class GenerateTrainingPlan {

        @Test
        @WithMockUser
        @DisplayName("should generate training plan with valid request")
        void shouldGenerateTrainingPlanWithValidRequest() throws Exception {
            // Given
            String expectedResponse = """
                {
                    "description": "4-week running plan for intermediate runners",
                    "weeks": [
                        {
                            "week": 1,
                            "totalDistance": 20,
                            "description": "Base building week",
                            "workouts": [
                                {
                                    "day": "Mon",
                                    "type": "easy run",
                                    "distance": 5,
                                    "duration": 30,
                                    "description": "Easy pace run"
                                }
                            ]
                        }
                    ]
                }
                """;

            when(groqService.generateTrainingPlan(anyString(), anyString(), anyInt(), anyInt()))
                    .thenReturn(expectedResponse);

            Map<String, Object> request = Map.of(
                    "activityType", "running",
                    "experienceLevel", "intermediate",
                    "targetDistance", 10,
                    "weeks", 4
            );

            // When & Then
            mockMvc.perform(post("/api/v1/ai/generate-plan")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.description").exists())
                    .andExpect(jsonPath("$.weeks").isArray())
                    .andExpect(jsonPath("$.weeks[0].week").value(1))
                    .andExpect(jsonPath("$.weeks[0].workouts").isArray());
        }

        @Test
        @WithMockUser
        @DisplayName("should use default values when request parameters are missing")
        void shouldUseDefaultValuesWhenParametersMissing() throws Exception {
            // Given
            String expectedResponse = """
                {
                    "description": "Default plan",
                    "weeks": []
                }
                """;

            when(groqService.generateTrainingPlan(eq("running"), eq("intermediate"), eq(10), eq(4)))
                    .thenReturn(expectedResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/ai/generate-plan")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("should handle string values for numeric parameters")
        void shouldHandleStringValuesForNumericParameters() throws Exception {
            // Given
            String expectedResponse = """
                {
                    "description": "Custom plan",
                    "weeks": []
                }
                """;

            when(groqService.generateTrainingPlan(anyString(), anyString(), eq(15), eq(8)))
                    .thenReturn(expectedResponse);

            String requestJson = """
                {
                    "activityType": "cycling",
                    "experienceLevel": "beginner",
                    "targetDistance": "15",
                    "weeks": "8"
                }
                """;

            // When & Then
            mockMvc.perform(post("/api/v1/ai/generate-plan")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 401 when user is not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // Given
            Map<String, Object> request = Map.of(
                    "activityType", "running",
                    "targetDistance", 10,
                    "weeks", 4
            );

            // When & Then
            mockMvc.perform(post("/api/v1/ai/generate-plan")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }
}
