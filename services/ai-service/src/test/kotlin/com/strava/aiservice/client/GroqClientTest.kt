package com.strava.aiservice.client

import com.strava.aiservice.exception.ApiRequestException
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
    private val client = GroqClient(
        rest = rest,
        apiUrl = "https://api.groq.com/openai/v1/chat/completions",
        apiKey = "test-api-key",
        model = "test-model"
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
}
