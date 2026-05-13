package com.strava.aiservice.client

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * Standalone-profile replacement for GroqClient — returns canned, prompt-
 * aware responses so smoke tests can exercise the full controller path
 * without an outbound network call or a real API key.
 */
@Component
@Profile("standalone")
class MockChatCompletionClient : ChatCompletionClient {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun complete(systemPrompt: String, userPrompt: String): String {
        log.info("[mock] complete called — prompt preview: '{}'", userPrompt.take(80))
        return when {
            userPrompt.contains("analyze", ignoreCase = true) ->
                "Solid run! Your average pace looks consistent. Try to add one tempo session " +
                "next week to push your 5k threshold."
            userPrompt.contains("training tip", ignoreCase = true) ->
                "Recovery days matter more than hero workouts — aim for 80% easy, 20% hard."
            userPrompt.contains("this week", ignoreCase = true)
                || userPrompt.contains("summarize", ignoreCase = true) ->
                "Strong week — 4 sessions, ~32 km total. Pace trend improving. Rest one full " +
                "day before your next long run."
            else ->
                "Here's a thought: consistency beats intensity. Keep showing up."
        }
    }
}
