package com.strava.aiservice.client

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import io.github.resilience4j.timelimiter.annotation.TimeLimiter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

/**
 * Wraps the Groq Chat Completions endpoint with three layers of protection,
 * stacked in this order (outermost first):
 *
 *   @TimeLimiter      — caps a single call at 8s so a hung connection
 *                       can't tie up a request thread indefinitely.
 *   @CircuitBreaker   — opens after sustained Groq failures so we stop
 *                       hitting an already-unreliable provider. Falls back
 *                       to a static "AI insights are temporarily unavailable"
 *                       string — keeps the surrounding UX usable.
 *   @Retry            — short-burst retries (2 tries, 300ms backoff) absorb
 *                       transient blips without exposing them to callers.
 *
 * Stateless and synchronous on purpose — for a streaming/SSE response we'd
 * switch to WebClient + Flux. That's a Phase 9 nice-to-have.
 */
@Component
@Profile("!standalone")
class GroqClient(
    private val rest: RestTemplate,
    @Value("\${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private val apiUrl: String,
    @Value("\${groq.api.key:}")
    private val apiKey: String,
    @Value("\${groq.model:openai/gpt-oss-120b}")
    private val model: String
) : ChatCompletionClient {

    private val log = LoggerFactory.getLogger(javaClass)

    @TimeLimiter(name = "groq", fallbackMethod = "fallback")
    @CircuitBreaker(name = "groq", fallbackMethod = "fallback")
    @Retry(name = "groq")
    override fun complete(systemPrompt: String, userPrompt: String): String {
        if (apiKey.isBlank()) {
            log.warn("GROQ_API_KEY not configured — returning fallback")
            return fallback(systemPrompt, userPrompt, IllegalStateException("missing key"))
        }
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBearerAuth(apiKey)
        }
        val body = GroqRequest(
            model = model,
            messages = listOf(
                GroqRequest.Message("system", systemPrompt),
                GroqRequest.Message("user", userPrompt)
            )
        )
        val response = rest.postForObject(apiUrl, HttpEntity(body, headers), GroqResponse::class.java)
        return response?.choices?.firstOrNull()?.message?.content
            ?: throw IllegalStateException("Groq returned an empty choices array")
    }

    @Suppress("UNUSED_PARAMETER")
    fun fallback(systemPrompt: String, userPrompt: String, ex: Throwable): String {
        log.warn("Groq fallback fired: {}", ex.message)
        return "AI insights are temporarily unavailable — please try again in a minute."
    }
}
