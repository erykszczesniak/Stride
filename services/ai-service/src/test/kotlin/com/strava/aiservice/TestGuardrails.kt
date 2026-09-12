package com.strava.aiservice

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.strava.aiservice.guard.AiGuardProperties
import com.strava.aiservice.guard.AiRateLimiter
import com.strava.aiservice.guard.Guardrails
import com.strava.aiservice.guard.PiiMasker
import com.strava.aiservice.guard.PromptGuard
import com.strava.aiservice.telemetry.AiCallRecorder
import io.micrometer.core.instrument.simple.SimpleMeterRegistry

/** Real guardrail and telemetry wiring on an in-memory registry, for unit tests that need the true behaviour. */
object TestGuardrails {

    fun properties(configure: AiGuardProperties.() -> Unit = {}): AiGuardProperties =
        AiGuardProperties().apply(configure)

    fun guardrails(
        properties: AiGuardProperties = properties(),
        registry: SimpleMeterRegistry = SimpleMeterRegistry(),
        rateLimiter: AiRateLimiter = AiRateLimiter(),
    ): Guardrails = Guardrails(properties, registry, rateLimiter, PromptGuard(properties.guard.maxRepeatedChars), PiiMasker())

    fun recorder(
        properties: AiGuardProperties = properties(),
        registry: SimpleMeterRegistry = SimpleMeterRegistry(),
    ): AiCallRecorder = AiCallRecorder(registry, properties, jacksonObjectMapper())
}
