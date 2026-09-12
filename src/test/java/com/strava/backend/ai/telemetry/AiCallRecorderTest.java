package com.strava.backend.ai.telemetry;

import com.strava.backend.TestGuardrails;
import com.strava.backend.ai.guard.AiGuardProperties;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("AiCallRecorder")
class AiCallRecorderTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final AiGuardProperties properties = TestGuardrails.properties(p -> {
        AiGuardProperties.ModelPrice price = new AiGuardProperties.ModelPrice();
        price.setInputPerMtok(10.0);
        price.setOutputPerMtok(20.0);
        p.getPricing().put("m1", price);
    });
    private final AiCallRecorder recorder = TestGuardrails.recorder(properties, registry);

    @Test
    @DisplayName("records tokens, duration, outcome and the cost from the price table")
    void recordsPricedCall() {
        double cost = recorder.record(AiCallRecorder.Call.builder()
                .endpoint(AiCallRecorder.Endpoint.CHAT).provider("anthropic").model("m1")
                .userId(7L).activityId(100L).inputTokens(1000L).outputTokens(500L)
                .duration(Duration.ofMillis(1200)).outcome(AiCallRecorder.Outcome.OK)
                .build());

        // (1000 × 10 + 500 × 20) / 1e6 USD
        assertThat(cost).isCloseTo(0.02, within(1e-9));
        assertThat(registry.counter("ai.requests", "endpoint", "chat", "provider", "anthropic", "model", "m1", "outcome", "ok").count()).isEqualTo(1.0);
        assertThat(registry.counter("ai.tokens", "provider", "anthropic", "model", "m1", "direction", "input").count()).isEqualTo(1000.0);
        assertThat(registry.counter("ai.tokens", "provider", "anthropic", "model", "m1", "direction", "output").count()).isEqualTo(500.0);
        assertThat(registry.counter("ai.request.cost.usd", "provider", "anthropic", "model", "m1").count()).isCloseTo(0.02, within(1e-9));
        Timer timer = registry.find("ai.request.duration").tag("endpoint", "chat").tag("outcome", "ok").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("an unpriced model is recorded with zero cost and no cost series")
    void unpricedModel() {
        double cost = recorder.record(AiCallRecorder.Call.builder()
                .endpoint(AiCallRecorder.Endpoint.GOAL).provider("groq").model("unknown-model")
                .userId(7L).inputTokens(10L).outputTokens(10L)
                .duration(Duration.ofMillis(50)).outcome(AiCallRecorder.Outcome.OK)
                .build());
        assertThat(cost).isZero();
        assertThat(registry.find("ai.request.cost.usd").counter()).isNull();
    }

    @Test
    @DisplayName("failures are counted by outcome and failover is counted separately")
    void failuresAndFailover() {
        recorder.record(AiCallRecorder.Call.builder()
                .endpoint(AiCallRecorder.Endpoint.CHAT).provider("groq").model("m1").userId(1L)
                .duration(Duration.ofMillis(5)).outcome(AiCallRecorder.Outcome.PROVIDER_ERROR)
                .error("IOException").failedOver(true)
                .build());
        assertThat(registry.counter("ai.requests", "endpoint", "chat", "provider", "groq", "model", "m1", "outcome", "provider_error").count()).isEqualTo(1.0);
        assertThat(registry.counter("ai.failover", "endpoint", "chat", "to", "groq").count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("recordFailover counts the decision on its own")
    void recordsFailoverDecision() {
        recorder.recordFailover(AiCallRecorder.Endpoint.CHAT, "groq");
        assertThat(registry.counter("ai.failover", "endpoint", "chat", "to", "groq").count()).isEqualTo(1.0);
    }
}
