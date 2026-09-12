package com.strava.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.backend.ai.guard.AiGuardProperties;
import com.strava.backend.ai.guard.AiRateLimiter;
import com.strava.backend.ai.guard.Guardrails;
import com.strava.backend.ai.guard.PiiMasker;
import com.strava.backend.ai.guard.PromptGuard;
import com.strava.backend.ai.telemetry.AiCallRecorder;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.function.Consumer;

/** Real guardrail and telemetry wiring on an in-memory registry, for unit tests that need the true behaviour. */
public final class TestGuardrails {

    private TestGuardrails() {
    }

    public static AiGuardProperties properties() {
        return new AiGuardProperties();
    }

    public static AiGuardProperties properties(Consumer<AiGuardProperties> configure) {
        AiGuardProperties p = new AiGuardProperties();
        configure.accept(p);
        return p;
    }

    public static Guardrails guardrails() {
        return guardrails(properties(), new SimpleMeterRegistry());
    }

    public static Guardrails guardrails(AiGuardProperties properties) {
        return guardrails(properties, new SimpleMeterRegistry());
    }

    public static Guardrails guardrails(AiGuardProperties properties, SimpleMeterRegistry registry) {
        return new Guardrails(properties, registry, new AiRateLimiter(),
                new PromptGuard(properties.getGuard().getMaxRepeatedChars()), new PiiMasker());
    }

    public static AiCallRecorder recorder() {
        return recorder(properties(), new SimpleMeterRegistry());
    }

    public static AiCallRecorder recorder(AiGuardProperties properties, SimpleMeterRegistry registry) {
        return new AiCallRecorder(registry, properties, new ObjectMapper());
    }
}
