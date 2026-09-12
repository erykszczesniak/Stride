package com.strava.aiservice.eval

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.strava.aiservice.TestGuardrails
import com.strava.aiservice.client.GroqClient
import com.strava.aiservice.goal.GroqService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestTemplate

/**
 * Live evaluation of goal / training-plan generation against the real Groq
 * API. The frontend parses the raw JSON string, so the contract is: valid
 * JSON, the exact keys, sane values. Runs only with GROQ_API_KEY set.
 */
@Tag("live-eval")
@EnabledIfEnvironmentVariable(named = "GROQ_API_KEY", matches = ".+")
@DisplayName("Live eval: Groq goal and plan generation")
class LiveGenerationEvalTest {

    private val mapper = jacksonObjectMapper()
    private val rest = RestTemplate(SimpleClientHttpRequestFactory().apply { setConnectTimeout(5000); setReadTimeout(30000) })
    private val client = GroqClient(
        rest = rest,
        apiUrl = System.getenv("GROQ_API_URL") ?: "https://api.groq.com/openai/v1/chat/completions",
        apiKey = System.getenv("GROQ_API_KEY"),
        model = System.getenv("GROQ_MODEL") ?: "openai/gpt-oss-120b",
        recorder = TestGuardrails.recorder()
    )
    private val service = GroqService(client, TestGuardrails.guardrails())

    private fun json(raw: String): JsonNode = mapper.readTree(raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```"))

    @Test
    @DisplayName("generate-goal returns the exact JSON contract with three milestones and tips")
    fun goalContract() {
        val node = json(service.generateGoal(1, "distance", "3months", "I want to run a 10K without stopping"))

        assertThat(node.fieldNames().asSequence().toSet()).containsExactlyInAnyOrder("smartGoal", "analysis", "milestones", "tips")
        assertThat(node["smartGoal"].asText()).hasSizeGreaterThan(20)
        assertThat(node["milestones"].size()).isBetween(2, 5)
        assertThat(node["tips"].size()).isBetween(2, 5)
        assertThat(node["smartGoal"].asText()).containsIgnoringCase("10")
    }

    @Test
    @DisplayName("generate-plan returns the requested number of weeks with allowed workout types")
    fun planContract() {
        val allowed = setOf("easy run", "tempo", "long run", "interval", "rest", "recovery")
        val node = json(service.generateTrainingPlan(1, "running", "intermediate", 10, 4))

        assertThat(node.fieldNames().asSequence().toSet()).containsExactlyInAnyOrder("description", "weeks")
        assertThat(node["weeks"].size()).isEqualTo(4)
        node["weeks"].forEachIndexed { i, week ->
            assertThat(week["week"].asInt()).isEqualTo(i + 1)
            assertThat(week["workouts"].size()).isBetween(3, 7)
            week["workouts"].forEach { w ->
                assertThat(w["type"].asText().lowercase()).isIn(allowed)
                assertThat(w["distance"].asDouble()).isBetween(0.0, 60.0)
            }
        }
    }
}
