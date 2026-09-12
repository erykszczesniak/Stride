package com.strava.aiservice.guard

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Guardrail settings for every LLM-backed endpoint, bound from `app.ai.*`.
 * Defaults are the production values; the standalone profile keeps them so
 * the smoke test exercises the real guard path.
 */
@Configuration
@ConfigurationProperties(prefix = "app.ai")
class AiGuardProperties {

    var rateLimit: RateLimit = RateLimit()
    var guard: Guard = Guard()
    var pii: Pii = Pii()

    /** USD per million tokens, keyed by model id as the provider reports it. */
    var pricing: MutableMap<String, ModelPrice> = mutableMapOf()

    class RateLimit {
        /** Chat turns one user may send per minute (persisted, provider-billed). */
        var chatPerMinute: Int = 10

        /** Goal / plan generations one user may request per minute. */
        var generationPerMinute: Int = 5
    }

    class Guard {
        var enabled: Boolean = true

        /** Longest run of the same character tolerated before the message is treated as noise. */
        var maxRepeatedChars: Int = 40
    }

    class Pii {
        var enabled: Boolean = true
    }

    class ModelPrice {
        var inputPerMtok: Double = 0.0
        var outputPerMtok: Double = 0.0
    }

    fun priceFor(model: String): ModelPrice? = pricing[model]
}
