package com.strava.aiservice.client

import com.strava.aiservice.TestGuardrails
import com.strava.aiservice.domain.ChatRole
import com.strava.aiservice.exception.ApiRequestException
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate

@DisplayName("GroqClient")
class GroqClientTest {

    private val rest: RestTemplate = mock()
    private val registry = SimpleMeterRegistry()
    private val client = GroqClient(
        rest = rest,
        apiUrl = "https://api.groq.com/openai/v1/chat/completions",
        apiKey = "test-api-key",
        model = "test-model",
        recorder = TestGuardrails.recorder(registry = registry)
    )

    @Test
    @DisplayName("calls Groq with the monolith's request shape (bearer auth, temp 0.7, max_tokens 2048)")
    fun callsGroqWithCorrectParameters() {
        whenever(rest.postForObject(any<String>(), any(), eq(GroqResponse::class.java)))
            .thenReturn(GroqResponse(listOf(GroqResponse.Choice(GroqRequest.Message("assistant", "Generated content")))))

        val result = client.complete("System prompt", "User prompt")

        assertThat(result).isEqualTo("Generated content")

        val entityCaptor = argumentCaptor<HttpEntity<GroqRequest>>()
        val urlCaptor = argumentCaptor<String>()
        org.mockito.kotlin.verify(rest).postForObject(urlCaptor.capture(), entityCaptor.capture(), eq(GroqResponse::class.java))

        assertThat(urlCaptor.firstValue).isEqualTo("https://api.groq.com/openai/v1/chat/completions")
        val entity = entityCaptor.firstValue
        assertThat(entity.headers.getFirst("Authorization")).isEqualTo("Bearer test-api-key")
        val body = entity.body!!
        assertThat(body.model).isEqualTo("test-model")
        assertThat(body.temperature).isEqualTo(0.7)
        assertThat(body.maxTokens).isEqualTo(2048)
        assertThat(body.messages).containsExactly(
            GroqRequest.Message("system", "System prompt"),
            GroqRequest.Message("user", "User prompt")
        )
    }

    @Test
    @DisplayName("maps transport failures to the monolith's generic 500")
    fun mapsTransportFailures() {
        whenever(rest.postForObject(any<String>(), any(), eq(GroqResponse::class.java)))
            .thenThrow(ResourceAccessException("connection refused"))

        assertThatThrownBy { client.complete("s", "u") }
            .isInstanceOf(ApiRequestException::class.java)
            .hasMessage("Failed to generate AI response")
            .extracting { (it as ApiRequestException).status }
            .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @Test
    @DisplayName("maps an empty choices array to the same generic 500")
    fun mapsEmptyChoices() {
        whenever(rest.postForObject(any<String>(), any(), eq(GroqResponse::class.java)))
            .thenReturn(GroqResponse(emptyList()))

        assertThatThrownBy { client.complete("s", "u") }
            .isInstanceOf(ApiRequestException::class.java)
            .hasMessage("Failed to generate AI response")
    }

    @Test
    @DisplayName("records tokens from the usage block and the outcome as metrics")
    fun recordsUsageMetrics() {
        whenever(rest.postForObject(any<String>(), any(), eq(GroqResponse::class.java)))
            .thenReturn(GroqResponse(listOf(GroqResponse.Choice(GroqRequest.Message("assistant", "ok"))), GroqResponse.Usage(120, 30)))

        client.complete("System prompt", "User prompt")

        assertThat(registry.counter("ai.tokens", "provider", "groq", "model", "test-model", "direction", "input").count()).isEqualTo(120.0)
        assertThat(registry.counter("ai.tokens", "provider", "groq", "model", "test-model", "direction", "output").count()).isEqualTo(30.0)
        assertThat(registry.counter("ai.requests", "endpoint", "goal", "provider", "groq", "model", "test-model", "outcome", "ok").count()).isEqualTo(1.0)
    }

    @Test
    @DisplayName("a transport failure is recorded as provider_error before the generic 500 surfaces")
    fun recordsFailureOutcome() {
        whenever(rest.postForObject(any<String>(), any(), eq(GroqResponse::class.java)))
            .thenThrow(ResourceAccessException("connection refused"))

        assertThatThrownBy { client.complete("s", "u") }.isInstanceOf(ApiRequestException::class.java)

        assertThat(registry.counter("ai.requests", "endpoint", "goal", "provider", "groq", "model", "test-model", "outcome", "provider_error").count()).isEqualTo(1.0)
    }

    @Test
    @DisplayName("chatCompletion replays the conversation as OpenAI-style roles behind the system prompt")
    fun chatCompletionMapsRoles() {
        whenever(rest.postForObject(any<String>(), any(), eq(GroqResponse::class.java)))
            .thenReturn(GroqResponse(listOf(GroqResponse.Choice(GroqRequest.Message("assistant", "degraded but present")))))

        val reply = client.chatCompletion("coach", listOf(
            ChatMessage(ChatRole.USER, "hi"),
            ChatMessage(ChatRole.ASSISTANT, "hello"),
            ChatMessage(ChatRole.USER, "how was my run?")
        ))

        assertThat(reply).isEqualTo("degraded but present")
        val entityCaptor = argumentCaptor<HttpEntity<GroqRequest>>()
        org.mockito.kotlin.verify(rest).postForObject(any<String>(), entityCaptor.capture(), eq(GroqResponse::class.java))
        assertThat(entityCaptor.firstValue.body!!.messages).containsExactly(
            GroqRequest.Message("system", "coach"),
            GroqRequest.Message("user", "hi"),
            GroqRequest.Message("assistant", "hello"),
            GroqRequest.Message("user", "how was my run?")
        )
        assertThat(registry.counter("ai.requests", "endpoint", "chat", "provider", "groq", "model", "test-model", "outcome", "ok").count()).isEqualTo(1.0)
    }

    @Test
    @DisplayName("isConfigured reflects the api key")
    fun isConfiguredReflectsKey() {
        assertThat(client.isConfigured()).isTrue()
        assertThat(GroqClient(rest, "u", "", "m", TestGuardrails.recorder()).isConfigured()).isFalse()
    }
}
