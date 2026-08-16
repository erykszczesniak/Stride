package com.strava.aiservice.client

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.messages.MessageCreateParams
import com.strava.aiservice.config.ClaudeProperties
import com.strava.aiservice.domain.ChatRole
import com.strava.aiservice.exception.ApiRequestException
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import com.anthropic.client.AnthropicClient as AnthropicSdkClient

/**
 * Anthropic Claude implementation backed by the official anthropic-java SDK —
 * port of the monolith's ClaudeServiceImpl.
 *
 * The SDK client is built lazily on first use (rather than as a bean) so the app boots even
 * when ANTHROPIC_API_KEY is unset — chat requests then fail fast with 503. The client is
 * settable so tests can inject a mock, mirroring how the project's other HTTP integrations
 * swap their RestTemplate in tests.
 */
@Component
@Profile("!standalone")
class AnthropicClient(private val properties: ClaudeProperties) : ClaudeChatClient {

    private val log = LoggerFactory.getLogger(javaClass)

    @Volatile
    private var client: AnthropicSdkClient? = null

    override fun isConfigured(): Boolean = properties.isConfigured()

    override fun chat(systemPrompt: String, conversation: List<ChatMessage>): String {
        if (!isConfigured()) {
            throw ApiRequestException("AI chat is not configured", HttpStatus.SERVICE_UNAVAILABLE)
        }
        try {
            val builder = MessageCreateParams.builder()
                .model(properties.model)
                .maxTokens(properties.maxTokens)
                .system(systemPrompt)

            for (message in conversation) {
                if (message.role == ChatRole.ASSISTANT) {
                    builder.addAssistantMessage(message.content)
                } else {
                    builder.addUserMessage(message.content)
                }
            }

            val response = anthropicClient().messages().create(builder.build())

            val text = response.content().stream()
                .flatMap { block -> block.text().stream() }
                .map { textBlock -> textBlock.text() }
                .reduce("") { acc, part -> acc + part }
                .trim()

            if (text.isEmpty()) {
                throw ApiRequestException("Claude returned an empty response", HttpStatus.BAD_GATEWAY)
            }
            return text
        } catch (e: ApiRequestException) {
            throw e
        } catch (e: Exception) {
            log.error("Claude chat request failed: {}", e.message, e)
            throw ApiRequestException("Failed to generate AI response", HttpStatus.BAD_GATEWAY)
        }
    }

    private fun anthropicClient(): AnthropicSdkClient {
        var local = client
        if (local == null) {
            synchronized(this) {
                local = client
                if (local == null) {
                    local = AnthropicOkHttpClient.builder()
                        .apiKey(properties.apiKey)
                        .build()
                    client = local
                }
            }
        }
        return local!!
    }

    /** Test seam: inject a mock SDK client instead of hitting the real API. */
    internal fun setClient(client: AnthropicSdkClient) {
        this.client = client
    }
}
