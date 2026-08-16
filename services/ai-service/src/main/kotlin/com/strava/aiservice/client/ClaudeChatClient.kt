package com.strava.aiservice.client

import com.strava.aiservice.domain.ChatRole

/** One turn handed to the chat model — role plus text (port of main's ClaudeService.ClaudeMessage). */
data class ChatMessage(val role: ChatRole, val content: String)

/**
 * Multi-turn sibling of [ChatCompletionClient] for the persisted activity
 * chat: the whole conversation is replayed on every request so Claude keeps
 * context. AnthropicClient in prod, MockChatCompletionClient in standalone.
 */
interface ClaudeChatClient {

    /** True only when an API key is configured — chat POSTs 503 otherwise. */
    fun isConfigured(): Boolean

    /**
     * Sends the system prompt plus the ordered conversation and returns the
     * assistant's reply text.
     *
     * @throws com.strava.aiservice.exception.ApiRequestException 503 when
     *         unconfigured, 502 when the provider fails or replies empty.
     */
    fun chat(systemPrompt: String, conversation: List<ChatMessage>): String
}
