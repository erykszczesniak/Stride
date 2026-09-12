package com.strava.backend.ai.telemetry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.backend.ai.guard.AiGuardProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * One record per provider call: what was asked, by whom, how long it took,
 * how many tokens it used and what it cost. Written twice — as Micrometer
 * series with bounded labels (provider, model, endpoint, outcome) for
 * dashboards and alerts, and as one JSON line on the {@code ai.audit} logger
 * for the per-request trail (userId, activityId, token counts, cost).
 *
 * <p>Cost is computed from {@code app.ai.pricing.<model>} in USD per million
 * tokens. A model with no price entry is recorded with cost 0 and a
 * {@code priced=false} field so the gap shows up instead of hiding.
 */
@Component
public class AiCallRecorder {

    public enum Endpoint {
        CHAT, GOAL, PLAN;

        public String label() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum Outcome {
        OK, PROVIDER_ERROR, EMPTY_RESPONSE, NOT_CONFIGURED, TIMEOUT;

        public String label() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    @Builder
    public record Call(
            Endpoint endpoint,
            String provider,
            String model,
            Long userId,
            Long activityId,
            Long inputTokens,
            Long outputTokens,
            Duration duration,
            Outcome outcome,
            boolean failedOver,
            String error) {
    }

    private static final Logger AUDIT = LoggerFactory.getLogger("ai.audit");

    private final MeterRegistry meterRegistry;
    private final AiGuardProperties properties;
    private final ObjectMapper objectMapper;

    public AiCallRecorder(MeterRegistry meterRegistry, AiGuardProperties properties, ObjectMapper objectMapper) {
        this.meterRegistry = meterRegistry;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /** One failover decision (primary unavailable, secondary about to be called). */
    public void recordFailover(Endpoint endpoint, String to) {
        meterRegistry.counter("ai.failover", "endpoint", endpoint.label(), "to", to).increment();
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("event", "ai.failover");
        line.put("endpoint", endpoint.label());
        line.put("to", to);
        audit(line);
    }

    /** Records the call and returns its cost in USD (0 when the model is unpriced). */
    public double record(Call call) {
        AiGuardProperties.ModelPrice price = properties.priceFor(call.model());
        long in = call.inputTokens() == null ? 0 : call.inputTokens();
        long out = call.outputTokens() == null ? 0 : call.outputTokens();
        double cost = price == null ? 0.0 : (in * price.getInputPerMtok() + out * price.getOutputPerMtok()) / 1_000_000.0;

        String endpoint = call.endpoint().label();
        String outcome = call.outcome().label();

        meterRegistry.counter("ai.requests", "endpoint", endpoint, "provider", call.provider(),
                "model", call.model(), "outcome", outcome).increment();
        Timer.builder("ai.request.duration")
                .tags("endpoint", endpoint, "provider", call.provider(), "outcome", outcome)
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(call.duration());
        if (call.inputTokens() != null) {
            meterRegistry.counter("ai.tokens", "provider", call.provider(), "model", call.model(), "direction", "input")
                    .increment(call.inputTokens());
        }
        if (call.outputTokens() != null) {
            meterRegistry.counter("ai.tokens", "provider", call.provider(), "model", call.model(), "direction", "output")
                    .increment(call.outputTokens());
        }
        if (cost > 0) {
            meterRegistry.counter("ai.request.cost.usd", "provider", call.provider(), "model", call.model()).increment(cost);
        }
        if (call.failedOver()) {
            meterRegistry.counter("ai.failover", "endpoint", endpoint, "to", call.provider()).increment();
        }

        Map<String, Object> line = new LinkedHashMap<>();
        line.put("event", "ai.call");
        line.put("endpoint", endpoint);
        line.put("provider", call.provider());
        line.put("model", call.model());
        line.put("userId", call.userId());
        line.put("activityId", call.activityId());
        line.put("inputTokens", call.inputTokens());
        line.put("outputTokens", call.outputTokens());
        line.put("latencyMs", call.duration().toMillis());
        line.put("outcome", outcome);
        line.put("failedOver", call.failedOver());
        line.put("costUsd", String.format(Locale.ROOT, "%.6f", cost));
        line.put("priced", price != null);
        line.put("error", call.error());
        audit(line);
        return cost;
    }

    private void audit(Map<String, Object> line) {
        try {
            AUDIT.info(objectMapper.writeValueAsString(line));
        } catch (JsonProcessingException e) {
            AUDIT.info(line.toString());
        }
    }
}
