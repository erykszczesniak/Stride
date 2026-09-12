package com.strava.aiservice.client

import com.strava.aiservice.TestGuardrails
import com.strava.aiservice.domain.ChatRole
import com.strava.aiservice.exception.ApiRequestException
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus

@DisplayName("FailoverChatClient")
class FailoverChatClientTest {

    private val primary: AnthropicClient = mock()
    private val secondary: GroqClient = mock()
    private val registry = SimpleMeterRegistry()
    private val properties = TestGuardrails.properties()
    private val client = FailoverChatClient(primary, secondary, properties, TestGuardrails.recorder(properties, registry))
    private val turn = listOf(ChatMessage(ChatRole.USER, "how was my run?"))

    @BeforeEach
    fun setUp() {
        whenever(primary.isConfigured()).thenReturn(true)
        whenever(secondary.isConfigured()).thenReturn(true)
    }

    private fun failovers(): Double = registry.find("ai.failover").tag("to", "groq").counter()?.count() ?: 0.0

    @Test
    @DisplayName("a healthy primary answers and nothing fails over")
    fun primaryAnswers() {
        whenever(primary.chat(any(), any())).thenReturn("Nice tempo.")

        assertThat(client.chat("sys", turn)).isEqualTo("Nice tempo.")
        verifyNoInteractions(secondary)
        assertThat(failovers()).isZero()
    }

    @Test
    @DisplayName("a 502 from the primary routes the same conversation to Groq and counts the failover")
    fun providerErrorFailsOver() {
        whenever(primary.chat(any(), any())).thenThrow(ApiRequestException("Failed to generate AI response", HttpStatus.BAD_GATEWAY))
        whenever(secondary.chatCompletion("sys", turn)).thenReturn("Groq says: solid.")

        assertThat(client.chat("sys", turn)).isEqualTo("Groq says: solid.")
        assertThat(failovers()).isEqualTo(1.0)
    }

    @Test
    @DisplayName("an open Anthropic breaker fails over without waiting")
    fun openBreakerFailsOver() {
        val breaker = CircuitBreaker.ofDefaults("anthropic")
        breaker.transitionToOpenState()
        whenever(primary.chat(any(), any())).thenThrow(CallNotPermittedException.createCallNotPermittedException(breaker))
        whenever(secondary.chatCompletion(any(), any())).thenReturn("degraded answer")

        assertThat(client.chat("sys", turn)).isEqualTo("degraded answer")
        assertThat(failovers()).isEqualTo(1.0)
    }

    @Test
    @DisplayName("an unconfigured primary goes straight to Groq")
    fun unconfiguredPrimaryUsesSecondary() {
        whenever(primary.isConfigured()).thenReturn(false)
        whenever(secondary.chatCompletion(any(), any())).thenReturn("groq")

        assertThat(client.isConfigured()).isTrue()
        assertThat(client.chat("sys", turn)).isEqualTo("groq")
        verify(primary, never()).chat(any(), any())
    }

    @Test
    @DisplayName("caller errors are not retried on the secondary")
    fun callerErrorsAreRethrown() {
        whenever(primary.chat(any(), any())).thenThrow(ApiRequestException("Activity not found", HttpStatus.NOT_FOUND))

        assertThatThrownBy { client.chat("sys", turn) }
            .isInstanceOf(ApiRequestException::class.java)
            .extracting { (it as ApiRequestException).status }
            .isEqualTo(HttpStatus.NOT_FOUND)
        verifyNoInteractions(secondary)
        assertThat(failovers()).isZero()
    }

    @Test
    @DisplayName("with failover disabled the primary's error surfaces unchanged")
    fun disabledFailoverRethrows() {
        properties.failover.enabled = false
        whenever(primary.chat(any(), any())).thenThrow(ApiRequestException("Failed to generate AI response", HttpStatus.BAD_GATEWAY))

        assertThatThrownBy { client.chat("sys", turn) }
            .extracting { (it as ApiRequestException).status }
            .isEqualTo(HttpStatus.BAD_GATEWAY)
        verifyNoInteractions(secondary)
    }

    @Test
    @DisplayName("neither provider configured is a 503, not a failover")
    fun nothingConfiguredIs503() {
        whenever(primary.isConfigured()).thenReturn(false)
        whenever(secondary.isConfigured()).thenReturn(false)

        assertThat(client.isConfigured()).isFalse()
        assertThatThrownBy { client.chat("sys", turn) }
            .extracting { (it as ApiRequestException).status }
            .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        assertThat(failovers()).isZero()
    }

    @Test
    @DisplayName("when the secondary fails as well the caller gets one 502")
    fun bothFailIs502() {
        whenever(primary.chat(any(), any())).thenThrow(ApiRequestException("Failed to generate AI response", HttpStatus.BAD_GATEWAY))
        whenever(secondary.chatCompletion(any(), any())).thenThrow(ApiRequestException("Failed to generate AI response", HttpStatus.INTERNAL_SERVER_ERROR))

        assertThatThrownBy { client.chat("sys", turn) }
            .extracting { (it as ApiRequestException).status }
            .isEqualTo(HttpStatus.BAD_GATEWAY)
        assertThat(failovers()).isEqualTo(1.0)
    }
}
