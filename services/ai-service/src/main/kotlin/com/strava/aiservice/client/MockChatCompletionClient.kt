package com.strava.aiservice.client

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * Standalone-profile replacement for GroqClient AND AnthropicClient — returns
 * canned, prompt-aware responses so smoke tests can exercise the full
 * controller paths (generate-goal, generate-plan, activity chat) without an
 * outbound network call or a real API key.
 */
@Component
@Profile("standalone")
class MockChatCompletionClient : ChatCompletionClient, ClaudeChatClient {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun complete(systemPrompt: String, userPrompt: String): String {
        log.info("[mock] complete called — prompt preview: '{}'", userPrompt.take(80))
        return when {
            systemPrompt.contains("SMART goals", ignoreCase = true) ->
                """
                {
                  "smartGoal": "Run a total of 60 km within the next month, building up gradually.",
                  "analysis": "Based on your input this target stretches you without risking injury.",
                  "milestones": ["Complete 12 km in week 1", "Hold 15 km/week through week 3", "Finish with a 10 km long run"],
                  "tips": ["Keep 80% of your runs easy", "Schedule one rest day after long runs", "Track your weekly totals"]
                }
                """.trimIndent()

            systemPrompt.contains("training plans", ignoreCase = true) ->
                """
                {
                  "description": "A progressive 4-week running plan building toward your target distance.",
                  "weeks": [
                    {
                      "week": 1,
                      "totalDistance": 20,
                      "description": "Base building week",
                      "workouts": [
                        {"day": "Mon", "type": "easy run", "distance": 5, "duration": 30, "description": "Relaxed pace, focus on form"},
                        {"day": "Wed", "type": "tempo", "distance": 6, "duration": 35, "description": "Comfortably hard middle 20 minutes"},
                        {"day": "Fri", "type": "rest", "distance": 0, "duration": 0, "description": "Full rest"},
                        {"day": "Sun", "type": "long run", "distance": 9, "duration": 60, "description": "Slow and steady"}
                      ]
                    }
                  ]
                }
                """.trimIndent()

            else ->
                "Here's a thought: consistency beats intensity. Keep showing up."
        }
    }

    override fun isConfigured(): Boolean = true

    override fun chat(systemPrompt: String, conversation: List<ChatMessage>): String {
        log.info(
            "[mock] chat called — {} turn(s), last: '{}'",
            conversation.size,
            conversation.lastOrNull()?.content?.take(80)
        )
        return "Solid session! Your pacing was consistent with the data provided — " +
            "recovery looks on track. Ask me about pacing, heart rate, or what to do next."
    }
}
