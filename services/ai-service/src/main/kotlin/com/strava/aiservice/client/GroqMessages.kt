package com.strava.aiservice.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Subset of the OpenAI-compatible chat-completions wire format that Groq
 * speaks. We only ship the fields we actually use and ignore everything
 * else on responses. Defaults mirror the monolith's GroqServiceImpl request
 * body: temperature 0.7, max_tokens 2048. `usage` is read for the token and
 * cost metrics (AiCallRecorder); it is optional so a provider that omits it
 * still works.
 */
data class GroqRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double = 0.7,
    @JsonProperty("max_tokens")
    val maxTokens: Int = 2048
) {
    data class Message(val role: String, val content: String)
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class GroqResponse(
    val choices: List<Choice>,
    val usage: Usage? = null
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Choice(val message: GroqRequest.Message, val index: Int? = null)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Usage(
        @JsonProperty("prompt_tokens") val promptTokens: Long? = null,
        @JsonProperty("completion_tokens") val completionTokens: Long? = null
    )
}
