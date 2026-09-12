package com.strava.aiservice.telemetry

import com.fasterxml.jackson.databind.ObjectMapper
import com.strava.aiservice.guard.AiGuardProperties
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * One record per provider call: what was asked, by whom, how long it took,
 * how many tokens it used and what it cost. Written twice — as Micrometer
 * series with bounded labels (provider, model, endpoint, outcome) for
 * dashboards and alerts, and as one JSON line on the `ai.audit` logger for
 * the per-request trail (userId, activityId, token counts, cost).
 *
 * Cost is computed from `app.ai.pricing.<model>` in USD per million tokens.
 * A model with no price entry is recorded with cost 0 and a `priced=false`
 * field so the gap shows up instead of hiding.
 */
@Component
class AiCallRecorder(
    private val meterRegistry: MeterRegistry,
    private val properties: AiGuardProperties,
    private val objectMapper: ObjectMapper,
) {

    enum class Endpoint { CHAT, GOAL, PLAN }
    enum class Outcome { OK, PROVIDER_ERROR, EMPTY_RESPONSE, NOT_CONFIGURED, TIMEOUT }

    data class Call(
        val endpoint: Endpoint,
        val provider: String,
        val model: String,
        val userId: Long?,
        val activityId: Long? = null,
        val inputTokens: Long? = null,
        val outputTokens: Long? = null,
        val duration: Duration,
        val outcome: Outcome,
        val failedOver: Boolean = false,
        val error: String? = null,
    )

    private val audit = LoggerFactory.getLogger("ai.audit")

    fun record(call: Call): Double {
        val price = properties.priceFor(call.model)
        val cost = if (price != null) {
            ((call.inputTokens ?: 0) * price.inputPerMtok + (call.outputTokens ?: 0) * price.outputPerMtok) / 1_000_000.0
        } else 0.0

        val endpoint = call.endpoint.name.lowercase()
        val outcome = call.outcome.name.lowercase()

        meterRegistry.counter("ai.requests", "endpoint", endpoint, "provider", call.provider, "model", call.model, "outcome", outcome).increment()
        Timer.builder("ai.request.duration")
            .tags("endpoint", endpoint, "provider", call.provider, "outcome", outcome)
            .publishPercentileHistogram()
            .register(meterRegistry)
            .record(call.duration)
        call.inputTokens?.let { meterRegistry.counter("ai.tokens", "provider", call.provider, "model", call.model, "direction", "input").increment(it.toDouble()) }
        call.outputTokens?.let { meterRegistry.counter("ai.tokens", "provider", call.provider, "model", call.model, "direction", "output").increment(it.toDouble()) }
        if (cost > 0) meterRegistry.counter("ai.request.cost.usd", "provider", call.provider, "model", call.model).increment(cost)
        if (call.failedOver) meterRegistry.counter("ai.failover", "endpoint", endpoint, "to", call.provider).increment()

        val line = linkedMapOf<String, Any?>(
            "event" to "ai.call",
            "endpoint" to endpoint,
            "provider" to call.provider,
            "model" to call.model,
            "userId" to call.userId,
            "activityId" to call.activityId,
            "inputTokens" to call.inputTokens,
            "outputTokens" to call.outputTokens,
            "latencyMs" to call.duration.toMillis(),
            "outcome" to outcome,
            "failedOver" to call.failedOver,
            "costUsd" to String.format(java.util.Locale.ROOT, "%.6f", cost),
            "priced" to (price != null),
            "error" to call.error,
        )
        audit.info(objectMapper.writeValueAsString(line))
        return cost
    }
}
