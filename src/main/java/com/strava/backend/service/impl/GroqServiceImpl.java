package com.strava.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.backend.ai.guard.Guardrails;
import com.strava.backend.ai.telemetry.AiCallContext;
import com.strava.backend.ai.telemetry.AiCallRecorder;
import com.strava.backend.configuration.GroqProperties;
import com.strava.backend.enums.ChatRole;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.service.ClaudeService.ClaudeMessage;
import com.strava.backend.service.GroqService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Groq Chat Completions client. Two entry points share one transport:
 * single-turn goal / plan generation (behind the guardrails) and the
 * multi-turn {@link #chatCompletion} used by {@link FailoverClaudeService}.
 * Every call is recorded by {@link AiCallRecorder} (latency, tokens from the
 * response {@code usage}, cost, outcome) against the caller in
 * {@link AiCallContext}. Provider failures surface as 502, a missing key as 503.
 */
@Service
@Slf4j
public class GroqServiceImpl implements GroqService {

    public static final String PROVIDER = "groq";

    private final GroqProperties properties;
    private final ObjectMapper objectMapper;
    private final AiCallRecorder recorder;
    private final Guardrails guardrails;
    private RestTemplate restTemplate;

    public GroqServiceImpl(GroqProperties properties, ObjectMapper objectMapper,
                           AiCallRecorder recorder, Guardrails guardrails) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.recorder = recorder;
        this.guardrails = guardrails;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(30_000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public boolean isConfigured() {
        return properties.isConfigured();
    }

    @Override
    public String generateCompletion(String systemPrompt, String userPrompt) {
        AiCallRecorder.Endpoint endpoint = systemPrompt.contains("training plan")
                ? AiCallRecorder.Endpoint.PLAN : AiCallRecorder.Endpoint.GOAL;
        return send(List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ), endpoint);
    }

    @Override
    public String chatCompletion(String systemPrompt, List<ClaudeMessage> conversation) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        for (ClaudeMessage m : conversation) {
            messages.add(Map.of("role", m.role() == ChatRole.ASSISTANT ? "assistant" : "user", "content", m.content()));
        }
        return send(messages, AiCallRecorder.Endpoint.CHAT);
    }

    @Override
    public String generateGoal(Long userId, String goalType, String timeframe, String description) {
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

        // Everything the user typed is in the prompt: rate limit, guard and mask it as one text.
        String admitted = guardrails.admit(userId, Guardrails.Endpoint.GENERATION, userPrompt);
        return AiCallContext.with(userId, null, () -> generateCompletion(systemPrompt, admitted));
    }

    @Override
    public String generateTrainingPlan(Long userId, String activityType, String experienceLevel,
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

        String admitted = guardrails.admit(userId, Guardrails.Endpoint.GENERATION, userPrompt);
        return AiCallContext.with(userId, null, () -> generateCompletion(systemPrompt, admitted));
    }

    private String send(List<Map<String, String>> messages, AiCallRecorder.Endpoint endpoint) {
        if (!isConfigured()) {
            record(endpoint, System.nanoTime(), null, AiCallRecorder.Outcome.NOT_CONFIGURED, null);
            throw new ApiRequestException("AI generation is not configured", HttpStatus.SERVICE_UNAVAILABLE);
        }
        long started = System.nanoTime();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(properties.getApiKey());

            Map<String, Object> requestBody = Map.of(
                    "model", properties.getModel(),
                    "messages", messages,
                    "temperature", 0.7,
                    "max_tokens", 2048
            );

            ResponseEntity<String> response = restTemplate.exchange(
                    properties.getApiUrl(), HttpMethod.POST, new HttpEntity<>(requestBody, headers), String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode choice = root.path("choices").path(0).path("message").path("content");
            if (choice.isMissingNode() || choice.asText().isBlank()) {
                record(endpoint, started, root.path("usage"), AiCallRecorder.Outcome.EMPTY_RESPONSE, null);
                throw new ApiRequestException("Failed to generate AI response", HttpStatus.BAD_GATEWAY);
            }
            record(endpoint, started, root.path("usage"), AiCallRecorder.Outcome.OK, null);
            return choice.asText();
        } catch (ApiRequestException e) {
            throw e;
        } catch (Exception e) {
            record(endpoint, started, null, AiCallRecorder.Outcome.PROVIDER_ERROR, e.getClass().getSimpleName());
            log.error("Error calling Groq API: {}", e.getMessage());
            throw new ApiRequestException("Failed to generate AI response", HttpStatus.BAD_GATEWAY);
        }
    }

    private void record(AiCallRecorder.Endpoint endpoint, long startedNanos, JsonNode usage,
                        AiCallRecorder.Outcome outcome, String error) {
        AiCallContext.Ctx ctx = AiCallContext.current();
        recorder.record(AiCallRecorder.Call.builder()
                .endpoint(endpoint)
                .provider(PROVIDER)
                .model(properties.getModel())
                .userId(ctx == null ? null : ctx.userId())
                .activityId(ctx == null ? null : ctx.activityId())
                .inputTokens(tokens(usage, "prompt_tokens"))
                .outputTokens(tokens(usage, "completion_tokens"))
                .duration(Duration.ofNanos(System.nanoTime() - startedNanos))
                .outcome(outcome)
                .error(error)
                .build());
    }

    private static Long tokens(JsonNode usage, String field) {
        if (usage == null || !usage.hasNonNull(field)) return null;
        return usage.get(field).asLong();
    }

    /** Test seam — lets tests inject a mocked RestTemplate. */
    void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
}
