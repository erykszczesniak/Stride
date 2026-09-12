package com.strava.aiservice.client

import com.strava.aiservice.exception.ApiRequestException
import com.strava.aiservice.guard.AiGuardProperties
import com.strava.aiservice.telemetry.AiCallRecorder
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

/**
 * The chat client the service layer actually uses: Anthropic first, Groq
 * when Anthropic is unavailable. "Unavailable" means a provider-side failure
 * (502 from the Anthropic client, its breaker open, or no key configured) —
 * never a caller error, which is rethrown untouched.
 *
 * The secondary answer is a degraded one (a different model, no Claude
 * system-prompt tuning) but it keeps the coach responding during a provider
 * incident. Every failover is counted (`ai_failover_total`) and shows up on
 * the cost dashboard as Groq spend on the chat endpoint.
 */
@Primary
@Component
@Profile("!standalone")
class FailoverChatClient(
    private val primary: AnthropicClient,
    private val secondary: GroqClient,
    private val properties: AiGuardProperties,
    private val recorder: AiCallRecorder,
) : ClaudeChatClient {

    private val log = LoggerFactory.getLogger(javaClass)

    private fun secondaryAvailable(): Boolean = properties.failover.enabled && secondary.isConfigured()

    override fun isConfigured(): Boolean = primary.isConfigured() || secondaryAvailable()

    override fun chat(systemPrompt: String, conversation: List<ChatMessage>): String {
        if (primary.isConfigured()) {
            try {
                return primary.chat(systemPrompt, conversation)
            } catch (e: Exception) {
                if (!shouldFailOver(e) || !secondaryAvailable()) throw e
                log.warn("Anthropic chat unavailable ({}); failing over to Groq", describe(e))
            }
        } else if (!secondaryAvailable()) {
            throw ApiRequestException("AI chat is not configured", HttpStatus.SERVICE_UNAVAILABLE)
        }

        recorder.recordFailover(AiCallRecorder.Endpoint.CHAT, GroqClient.PROVIDER)
        try {
            return secondary.chatCompletion(systemPrompt, conversation)
        } catch (e: Exception) {
            log.error("Groq failover chat failed too: {}", describe(e))
            throw ApiRequestException("Failed to generate AI response", HttpStatus.BAD_GATEWAY)
        }
    }

    /** Provider trouble fails over; anything that is the caller's problem does not. */
    private fun shouldFailOver(e: Exception): Boolean = when (e) {
        is CallNotPermittedException -> true
        is ApiRequestException -> e.status == HttpStatus.BAD_GATEWAY || e.status == HttpStatus.SERVICE_UNAVAILABLE
        else -> false
    }

    private fun describe(e: Exception): String =
        if (e is ApiRequestException) "${e.status.value()} ${e.message}" else e.javaClass.simpleName
}
