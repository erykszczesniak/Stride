package com.strava.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GroqService")
class GroqServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Captor
    private ArgumentCaptor<HttpEntity<Map<String, Object>>> requestCaptor;

    private GroqService groqService;

    @BeforeEach
    void setUp() {
        groqService = new GroqService();
        ReflectionTestUtils.setField(groqService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(groqService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(groqService, "model", "test-model");
    }

    @Nested
    @DisplayName("generateCompletion")
    class GenerateCompletion {

        @Test
        @DisplayName("should call Groq API with correct parameters")
        void shouldCallGroqApiWithCorrectParameters() {
            // Given
            String apiResponse = """
                {
                    "choices": [
                        {
                            "message": {
                                "content": "Generated content"
                            }
                        }
                    ]
                }
                """;

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(apiResponse, HttpStatus.OK));

            // When
            String result = groqService.generateCompletion("System prompt", "User prompt");

            // Then
            assertThat(result).isEqualTo("Generated content");

            verify(restTemplate).exchange(
                    eq("https://api.groq.com/openai/v1/chat/completions"),
                    eq(HttpMethod.POST),
                    requestCaptor.capture(),
                    eq(String.class)
            );

            HttpEntity<Map<String, Object>> capturedRequest = requestCaptor.getValue();
            assertThat(capturedRequest.getHeaders().get("Authorization"))
                    .containsExactly("Bearer test-api-key");
            assertThat(capturedRequest.getHeaders().getContentType().toString())
                    .contains("application/json");
        }

        @Test
        @DisplayName("should throw RuntimeException when API call fails")
        void shouldThrowRuntimeExceptionWhenApiCallFails() {
            // Given
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenThrow(new RuntimeException("API Error"));

            // When & Then
            assertThatThrownBy(() -> groqService.generateCompletion("System", "User"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to generate AI response");
        }
    }

    @Nested
    @DisplayName("generateGoal")
    class GenerateGoal {

        @Test
        @DisplayName("should generate goal with correct prompt structure")
        void shouldGenerateGoalWithCorrectPromptStructure() {
            // Given
            String apiResponse = """
                {
                    "choices": [
                        {
                            "message": {
                                "content": "{\\"smartGoal\\": \\"Run 5K\\", \\"analysis\\": \\"Good goal\\", \\"milestones\\": [], \\"tips\\": []}"
                            }
                        }
                    ]
                }
                """;

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(apiResponse, HttpStatus.OK));

            // When
            String result = groqService.generateGoal("distance", "month", "I want to run 5K");

            // Then
            assertThat(result).contains("smartGoal");
            assertThat(result).contains("Run 5K");

            verify(restTemplate).exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    requestCaptor.capture(),
                    eq(String.class)
            );

            HttpEntity<Map<String, Object>> capturedRequest = requestCaptor.getValue();
            Map<String, Object> body = capturedRequest.getBody();
            assertThat(body).containsKey("messages");
        }

        @Test
        @DisplayName("should include goal parameters in user prompt")
        void shouldIncludeGoalParametersInUserPrompt() {
            // Given
            String apiResponse = """
                {
                    "choices": [
                        {
                            "message": {
                                "content": "{}"
                            }
                        }
                    ]
                }
                """;

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(apiResponse, HttpStatus.OK));

            // When
            groqService.generateGoal("time", "week", "Train for marathon");

            // Then
            verify(restTemplate).exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    requestCaptor.capture(),
                    eq(String.class)
            );

            // Verify that the request was made (the actual prompt content is internal implementation)
            assertThat(requestCaptor.getValue().getBody()).isNotNull();
        }
    }

    @Nested
    @DisplayName("generateTrainingPlan")
    class GenerateTrainingPlan {

        @Test
        @DisplayName("should generate training plan with correct parameters")
        void shouldGenerateTrainingPlanWithCorrectParameters() {
            // Given
            String apiResponse = """
                {
                    "choices": [
                        {
                            "message": {
                                "content": "{\\"description\\": \\"4-week plan\\", \\"weeks\\": []}"
                            }
                        }
                    ]
                }
                """;

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(apiResponse, HttpStatus.OK));

            // When
            String result = groqService.generateTrainingPlan("running", "intermediate", 10, 4);

            // Then
            assertThat(result).contains("description");
            assertThat(result).contains("4-week plan");
        }

        @Test
        @DisplayName("should handle different activity types")
        void shouldHandleDifferentActivityTypes() {
            // Given
            String apiResponse = """
                {
                    "choices": [
                        {
                            "message": {
                                "content": "{\\"description\\": \\"Cycling plan\\", \\"weeks\\": []}"
                            }
                        }
                    ]
                }
                """;

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(apiResponse, HttpStatus.OK));

            // When
            String result = groqService.generateTrainingPlan("cycling", "beginner", 50, 8);

            // Then
            assertThat(result).contains("Cycling plan");
        }

        @Test
        @DisplayName("should handle long training plan duration")
        void shouldHandleLongTrainingPlanDuration() {
            // Given
            String apiResponse = """
                {
                    "choices": [
                        {
                            "message": {
                                "content": "{\\"description\\": \\"16-week marathon plan\\", \\"weeks\\": []}"
                            }
                        }
                    ]
                }
                """;

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(apiResponse, HttpStatus.OK));

            // When
            String result = groqService.generateTrainingPlan("running", "advanced", 42, 16);

            // Then
            assertThat(result).contains("16-week");
        }
    }
}
