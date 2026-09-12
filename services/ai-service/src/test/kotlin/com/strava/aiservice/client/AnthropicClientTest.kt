package com.strava.aiservice.client

import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageCreateParams
import com.strava.aiservice.TestGuardrails
import com.strava.aiservice.config.ClaudeProperties
import com.strava.aiservice.domain.ChatRole
import com.strava.aiservice.exception.ApiRequestException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import com.anthropic.client.AnthropicClient as AnthropicSdkClient

/** Port of the monolith's ClaudeServiceImplTest — SDK mocked through the same test seam. */
@DisplayName("AnthropicClient")
class AnthropicClientTest {

    private lateinit var properties: ClaudeProperties
    private lateinit var client: AnthropicClient

    @BeforeEach
    fun setUp() {
        properties = ClaudeProperties()
        client = AnthropicClient(properties, TestGuardrails.recorder())
    }

    @Test
    @DisplayName("isConfigured reflects whether an api key is present")
    fun isConfiguredReflectsApiKey() {
        assertThat(client.isConfigured()).isFalse()
        properties.apiKey = "sk-test"
        assertThat(client.isConfigured()).isTrue()
    }

    @Test
    @DisplayName("chat throws 503 when not configured")
    fun chatThrows503WhenNotConfigured() {
        assertThatThrownBy { client.chat("system", listOf(ChatMessage(ChatRole.USER, "hi"))) }
            .isInstanceOf(ApiRequestException::class.java)
            .hasMessage("AI chat is not configured")
            .extracting { (it as ApiRequestException).status }
            .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
    }

    @Test
    @DisplayName("chat wraps SDK failures as 502 Bad Gateway")
    fun chatWrapsSdkFailures() {
        properties.apiKey = "sk-test"

        val sdk = mock<AnthropicSdkClient>(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        whenever(sdk.messages().create(any<MessageCreateParams>()))
            .thenThrow(RuntimeException("network down"))
        client.setClient(sdk)

        assertThatThrownBy { client.chat("sys", listOf(ChatMessage(ChatRole.USER, "hi"))) }
            .isInstanceOf(ApiRequestException::class.java)
            .hasMessage("Failed to generate AI response")
            .extracting { (it as ApiRequestException).status }
            .isEqualTo(HttpStatus.BAD_GATEWAY)
    }

    @Test
    @DisplayName("chat maps an empty Claude reply to 502")
    fun chatMapsEmptyReplyTo502() {
        properties.apiKey = "sk-test"

        val emptyMessage = mock<Message>()
        whenever(emptyMessage.content()).thenReturn(emptyList())
        val sdk = mock<AnthropicSdkClient>(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        whenever(sdk.messages().create(any<MessageCreateParams>())).thenReturn(emptyMessage)
        client.setClient(sdk)

        assertThatThrownBy { client.chat("sys", listOf(ChatMessage(ChatRole.USER, "hi"))) }
            .isInstanceOf(ApiRequestException::class.java)
            .hasMessage("Claude returned an empty response")
            .extracting { (it as ApiRequestException).status }
            .isEqualTo(HttpStatus.BAD_GATEWAY)
    }
}
