package com.strava.aiservice.guard

import com.strava.aiservice.exception.ApiRequestException
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

/**
 * The single entry point the service layer calls before any text reaches a
 * provider: rate limit → prompt guard → PII masking. Order matters — a
 * rate-limited user must not get free guard evaluations, and the guard must
 * see the original text (masking could hide an injection inside an email
 * local part). Every outcome is a metric with bounded labels.
 */
@Component
class Guardrails(
    private val properties: AiGuardProperties,
    private val meterRegistry: MeterRegistry,
    private val rateLimiter: AiRateLimiter,
    private val promptGuard: PromptGuard,
    private val piiMasker: PiiMasker,
) {

    enum class Endpoint(val bucket: String) { CHAT("chat"), GENERATION("generation") }

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Admits `text` from `userId` for `endpoint` and returns the version that
     * may be sent to a provider and persisted (PII masked).
     *
     * @throws ApiRequestException 429 when the user is over their per-minute
     *         budget, 400 when the guard rejects the text.
     */
    fun admit(userId: Long, endpoint: Endpoint, text: String): String {
        val limit = when (endpoint) {
            Endpoint.CHAT -> properties.rateLimit.chatPerMinute
            Endpoint.GENERATION -> properties.rateLimit.generationPerMinute
        }
        if (!rateLimiter.tryAcquire(endpoint.bucket, userId, limit)) {
            meterRegistry.counter("ai.rate_limited", "endpoint", endpoint.bucket).increment()
            log.info("ai.guard rate_limited endpoint={} userId={}", endpoint.bucket, userId)
            throw ApiRequestException(
                "Too many AI requests — limit is $limit per minute for ${endpoint.bucket}",
                HttpStatus.TOO_MANY_REQUESTS
            )
        }

        if (properties.guard.enabled) {
            when (val decision = promptGuard.check(text)) {
                is PromptGuard.Decision.Blocked -> {
                    meterRegistry.counter("ai.guard.decisions", "decision", "blocked", "reason", decision.reason.name.lowercase()).increment()
                    // Never log the text itself: it is user content and may contain PII.
                    log.info("ai.guard blocked endpoint={} userId={} reason={} length={}", endpoint.bucket, userId, decision.reason, text.length)
                    throw ApiRequestException(decision.reason.userMessage, HttpStatus.BAD_REQUEST)
                }
                PromptGuard.Decision.Allowed ->
                    meterRegistry.counter("ai.guard.decisions", "decision", "allowed", "reason", "none").increment()
            }
        }

        if (!properties.pii.enabled) return text
        val masked = piiMasker.mask(text)
        masked.counts.forEach { (type, n) ->
            meterRegistry.counter("ai.pii.masked", "type", type.name.lowercase()).increment(n.toDouble())
        }
        if (masked.total > 0) {
            log.info("ai.guard pii_masked endpoint={} userId={} counts={}", endpoint.bucket, userId, masked.counts)
        }
        return masked.text
    }
}
