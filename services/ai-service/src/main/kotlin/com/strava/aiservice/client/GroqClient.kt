package com.strava.aiservice.client

import com.strava.aiservice.exception.ApiRequestException
import com.strava.aiservice.telemetry.AiCallContext
import com.strava.aiservice.telemetry.AiCallRecorder
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.time.Duration

/**
 * Wraps the Groq Chat Completions endpoint. Error behavior mirrors the
 * monolith's GroqServiceImpl: any failure (bad key, network, empty choices)
 * surfaces as a single "Failed to generate AI response" 500 — no canned
 * fallback text, the client sees the error.
 *
 * Resilience layers on top of that (outermost first):
 *   @CircuitBreaker — opens after sustained Groq failures so we stop hitting
 *                     an already-unreliable provider (open circuit => the
 *                     CallNotPermittedException maps to the same generic 500
 *                     via GlobalExceptionHandler).
 *   @Retry          — short-burst retries absorb transient blips.
 * RestTemplate timeouts (3s connect / 7s read) bound each attempt.
 *
 * Every attempt is recorded by AiCallRecorder (latency, tokens from the
 * `usage` block, cost, outcome) against the caller in AiCallContext.
 */
@Component
@Profile("!standalone")
class GroqClient(
    @Qualifier("rest") private val rest: RestTemplate,
    @Value("\${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private val apiUrl: String,
    @Value("\${groq.api-key:}")
    private val apiKey: String,
    @Value("\${groq.model:openai/gpt-oss-120b}")
    private val model: String,
    private val recorder: AiCallRecorder,
) : ChatCompletionClient {

    private val log = LoggerFactory.getLogger(javaClass)

    @CircuitBreaker(name = "groq")
    @Retry(name = "groq")
    override fun complete(systemPrompt: String, userPrompt: String): String {
        val started = System.nanoTime()
        val ctx = AiCallContext.current()
        val endpoint = if (systemPrompt.contains("training plans")) AiCallRecorder.Endpoint.PLAN else AiCallRecorder.Endpoint.GOAL
        try {
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
            val content = response?.choices?.firstOrNull()?.message?.content
                ?: throw IllegalStateException("Groq returned an empty choices array")
            recorder.record(
                AiCallRecorder.Call(
                    endpoint = endpoint, provider = PROVIDER, model = model,
                    userId = ctx?.userId, activityId = ctx?.activityId,
                    inputTokens = response.usage?.promptTokens, outputTokens = response.usage?.completionTokens,
                    duration = Duration.ofNanos(System.nanoTime() - started), outcome = AiCallRecorder.Outcome.OK
                )
            )
            return content
        } catch (e: Exception) {
            recorder.record(
                AiCallRecorder.Call(
                    endpoint = endpoint, provider = PROVIDER, model = model,
                    userId = ctx?.userId, activityId = ctx?.activityId,
                    duration = Duration.ofNanos(System.nanoTime() - started),
                    outcome = if (e is IllegalStateException) AiCallRecorder.Outcome.EMPTY_RESPONSE else AiCallRecorder.Outcome.PROVIDER_ERROR,
                    error = e.javaClass.simpleName
                )
            )
            // Same passthrough as main: log the cause, surface one generic failure.
            log.error("Error calling Groq API: {}", e.message)
            throw ApiRequestException("Failed to generate AI response", HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    companion object {
        const val PROVIDER = "groq"
    }
}
