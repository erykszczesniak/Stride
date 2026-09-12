package com.strava.backend.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.backend.TestGuardrails;
import com.strava.backend.configuration.GroqProperties;
import com.strava.backend.service.impl.GroqServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Live evaluation of goal / training-plan generation against the real Groq
 * API. The frontend parses the raw JSON string, so the contract is: valid
 * JSON, the exact keys, sane values. Runs only with GROQ_API_KEY set.
 */
@Tag("live-eval")
@EnabledIfEnvironmentVariable(named = "GROQ_API_KEY", matches = ".+")
@DisplayName("Live eval: Groq goal and plan generation")
class LiveGenerationEvalTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final GroqServiceImpl service;

    LiveGenerationEvalTest() {
        GroqProperties properties = new GroqProperties();
        properties.setApiKey(System.getenv("GROQ_API_KEY"));
        String model = System.getenv("GROQ_MODEL");
        if (model != null && !model.isBlank()) properties.setModel(model);
        String url = System.getenv("GROQ_API_URL");
        if (url != null && !url.isBlank()) properties.setApiUrl(url);
        service = new GroqServiceImpl(properties, mapper, TestGuardrails.recorder(), TestGuardrails.guardrails());
    }

    private JsonNode json(String raw) throws Exception {
        String s = raw.trim();
        if (s.startsWith("```json")) s = s.substring(7);
        if (s.startsWith("```")) s = s.substring(3);
        if (s.endsWith("```")) s = s.substring(0, s.length() - 3);
        return mapper.readTree(s.trim());
    }

    private static Set<String> fields(JsonNode node) {
        Set<String> out = new HashSet<>();
        node.fieldNames().forEachRemaining(out::add);
        return out;
    }

    @Test
    @DisplayName("generate-goal returns the exact JSON contract with three milestones and tips")
    void goalContract() throws Exception {
        JsonNode node = json(service.generateGoal(1L, "distance", "3months", "I want to run a 10K without stopping"));

        assertThat(fields(node)).containsExactlyInAnyOrder("smartGoal", "analysis", "milestones", "tips");
        assertThat(node.get("smartGoal").asText()).hasSizeGreaterThan(20);
        assertThat(node.get("milestones").size()).isBetween(2, 5);
        assertThat(node.get("tips").size()).isBetween(2, 5);
        assertThat(node.get("smartGoal").asText()).containsIgnoringCase("10");
    }

    @Test
    @DisplayName("generate-plan returns the requested number of weeks with allowed workout types")
    void planContract() throws Exception {
        Set<String> allowed = Set.of("easy run", "tempo", "long run", "interval", "rest", "recovery");
        JsonNode node = json(service.generateTrainingPlan(1L, "running", "intermediate", 10, 4));

        assertThat(fields(node)).containsExactlyInAnyOrder("description", "weeks");
        assertThat(node.get("weeks").size()).isEqualTo(4);
        for (int i = 0; i < node.get("weeks").size(); i++) {
            JsonNode week = node.get("weeks").get(i);
            assertThat(week.get("week").asInt()).isEqualTo(i + 1);
            assertThat(week.get("workouts").size()).isBetween(3, 7);
            for (JsonNode w : week.get("workouts")) {
                assertThat(w.get("type").asText().toLowerCase()).isIn(allowed);
                assertThat(w.get("distance").asDouble()).isBetween(0.0, 60.0);
            }
        }
    }
}
