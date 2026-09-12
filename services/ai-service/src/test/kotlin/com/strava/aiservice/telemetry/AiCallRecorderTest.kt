package com.strava.aiservice.telemetry

import com.strava.aiservice.TestGuardrails
import com.strava.aiservice.guard.AiGuardProperties
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration

@DisplayName("AiCallRecorder")
class AiCallRecorderTest {

    private val registry = SimpleMeterRegistry()
    private val properties = TestGuardrails.properties {
        pricing["m1"] = AiGuardProperties.ModelPrice().apply { inputPerMtok = 10.0; outputPerMtok = 20.0 }
    }
    private val recorder = TestGuardrails.recorder(properties, registry)

    @Test
    @DisplayName("records tokens, duration, outcome and the cost from the price table")
    fun recordsPricedCall() {
        val cost = recorder.record(
            AiCallRecorder.Call(
                endpoint = AiCallRecorder.Endpoint.CHAT, provider = "anthropic", model = "m1",
                userId = 7, activityId = 100, inputTokens = 1000, outputTokens = 500,
                duration = Duration.ofMillis(1200), outcome = AiCallRecorder.Outcome.OK
            )
        )

        // (1000 × 10 + 500 × 20) / 1e6 USD
        assertThat(cost).isCloseTo(0.02, within(1e-9))
        assertThat(registry.counter("ai.requests", "endpoint", "chat", "provider", "anthropic", "model", "m1", "outcome", "ok").count()).isEqualTo(1.0)
        assertThat(registry.counter("ai.tokens", "provider", "anthropic", "model", "m1", "direction", "input").count()).isEqualTo(1000.0)
        assertThat(registry.counter("ai.tokens", "provider", "anthropic", "model", "m1", "direction", "output").count()).isEqualTo(500.0)
        assertThat(registry.counter("ai.request.cost.usd", "provider", "anthropic", "model", "m1").count()).isCloseTo(0.02, within(1e-9))
        val timer = registry.find("ai.request.duration").tag("endpoint", "chat").tag("outcome", "ok").timer()
        assertThat(timer).isNotNull
        assertThat(timer!!.count()).isEqualTo(1)
    }

    @Test
    @DisplayName("an unpriced model is recorded with zero cost and no cost series")
    fun unpricedModel() {
        val cost = recorder.record(
            AiCallRecorder.Call(
                endpoint = AiCallRecorder.Endpoint.GOAL, provider = "groq", model = "unknown-model",
                userId = 7, inputTokens = 10, outputTokens = 10,
                duration = Duration.ofMillis(50), outcome = AiCallRecorder.Outcome.OK
            )
        )
        assertThat(cost).isZero()
        assertThat(registry.find("ai.request.cost.usd").counter()).isNull()
    }

    @Test
    @DisplayName("failures are counted by outcome and failover is counted separately")
    fun failuresAndFailover() {
        recorder.record(
            AiCallRecorder.Call(
                endpoint = AiCallRecorder.Endpoint.CHAT, provider = "groq", model = "m1", userId = 1,
                duration = Duration.ofMillis(5), outcome = AiCallRecorder.Outcome.PROVIDER_ERROR, error = "IOException", failedOver = true
            )
        )
        assertThat(registry.counter("ai.requests", "endpoint", "chat", "provider", "groq", "model", "m1", "outcome", "provider_error").count()).isEqualTo(1.0)
        assertThat(registry.counter("ai.failover", "endpoint", "chat", "to", "groq").count()).isEqualTo(1.0)
    }
}
