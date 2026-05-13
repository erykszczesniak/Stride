package com.strava.aiservice.client

/**
 * Strategy interface so the standalone profile can swap in a deterministic
 * mock without dragging the real Groq client (and its API-key requirement)
 * into local smoke tests.
 */
interface ChatCompletionClient {
    fun complete(systemPrompt: String, userPrompt: String): String
}
