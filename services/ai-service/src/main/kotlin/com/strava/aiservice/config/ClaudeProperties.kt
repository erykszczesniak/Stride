package com.strava.aiservice.config

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Anthropic Claude settings for the AI activity-analysis chat. Populated from env vars via
 * `app.claude.*` in application.yml. The api-key default is empty on purpose — the app
 * still boots without it, but the chat POST endpoint will return 503 and a WARN is logged
 * at startup. Port of the monolith's configuration/ClaudeProperties; the default model id
 * is `claude-opus-5` (current Anthropic default — main's `claude-opus-4-8` predates it).
 */
@Configuration
@ConfigurationProperties(prefix = "app.claude")
class ClaudeProperties {

    private val log = LoggerFactory.getLogger(javaClass)

    var apiKey: String = ""

    /** Model id — defaults to Claude Opus 5, the current Opus-tier model. */
    var model: String = "claude-opus-5"

    /** Upper bound on tokens generated per reply. Keeps a single analysis snappy and bounded. */
    var maxTokens: Long = 1024

    /** Returns true only when an api key is present. */
    fun isConfigured(): Boolean = apiKey.isNotBlank()

    @PostConstruct
    fun warnIfUnconfigured() {
        if (!isConfigured()) {
            log.warn(
                "Claude AI chat is not configured — set ANTHROPIC_API_KEY to enable it. " +
                    "POST /api/v1/ai/activities/{id}/chat will return 503."
            )
        } else {
            log.info("Claude AI chat configured (model={}, maxTokens={})", model, maxTokens)
        }
    }
}
