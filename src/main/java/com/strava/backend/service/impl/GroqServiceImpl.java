package com.strava.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.backend.service.GroqService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GroqServiceImpl implements GroqService {

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

    @Value("${groq.api-key}")
    private String apiKey;

    @Value("${groq.model}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GroqServiceImpl(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    @Override
    public String generateCompletion(String systemPrompt, String userPrompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.7,
                "max_tokens", 2048
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                GROQ_API_URL,
                HttpMethod.POST,
                entity,
                String.class
            );

            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            return jsonNode.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            log.error("Error calling Groq API: {}", e.getMessage());
            throw new RuntimeException("Failed to generate AI response", e);
        }
    }

    @Override
    public String generateGoal(String goalType, String timeframe, String description) {
        String systemPrompt = """
            You are a fitness coach AI that helps athletes create SMART goals.
            Respond ONLY with valid JSON in this exact format (no markdown, no explanation):
            {
              "smartGoal": "A specific, measurable goal statement",
              "analysis": "Why this goal works for the user based on their input",
              "milestones": ["Milestone 1", "Milestone 2", "Milestone 3"],
              "tips": ["Tip 1", "Tip 2", "Tip 3"]
            }
            """;

        String userPrompt = String.format("""
            Create a SMART fitness goal for:
            - Goal Type: %s
            - Timeframe: %s
            - User's description: %s

            Make it specific, achievable, and motivating.
            """, goalType, timeframe, description);

        return generateCompletion(systemPrompt, userPrompt);
    }

    @Override
    public String generateTrainingPlan(String activityType, String experienceLevel,
                                        int targetDistance, int weeks) {
        String systemPrompt = """
            You are a professional fitness coach AI that creates personalized training plans.
            Respond ONLY with valid JSON in this exact format (no markdown, no explanation):
            {
              "description": "Overview of the training plan",
              "weeks": [
                {
                  "week": 1,
                  "totalDistance": 40,
                  "description": "Week focus description",
                  "workouts": [
                    {
                      "day": "Mon",
                      "type": "easy run",
                      "distance": 8,
                      "duration": 45,
                      "description": "Workout description"
                    }
                  ]
                }
              ]
            }

            Workout types must be one of: "easy run", "tempo", "long run", "interval", "rest", "recovery"
            Include 4-5 workouts per week including rest days.
            """;

        String userPrompt = String.format("""
            Create a %d-week training plan for:
            - Activity: %s
            - Experience Level: %s
            - Target Distance: %d km

            Make it progressive, realistic, and include variety.
            """, weeks, activityType, experienceLevel, targetDistance);

        return generateCompletion(systemPrompt, userPrompt);
    }
}
