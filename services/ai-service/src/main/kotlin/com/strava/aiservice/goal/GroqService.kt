package com.strava.aiservice.goal

import com.strava.aiservice.client.ChatCompletionClient
import org.springframework.stereotype.Service

/**
 * Prompt composition for goal / training-plan generation — the prompts are the
 * monolith's GroqServiceImpl verbatim (they define the JSON contract the
 * frontend parses). Transport lives behind ChatCompletionClient (GroqClient in
 * prod, MockChatCompletionClient in standalone).
 */
@Service
class GroqService(private val chat: ChatCompletionClient) {

    fun generateCompletion(systemPrompt: String, userPrompt: String): String =
        chat.complete(systemPrompt, userPrompt)

    fun generateGoal(goalType: String, timeframe: String, description: String): String {
        val systemPrompt = """
            You are a fitness coach AI that helps athletes create SMART goals.
            Respond ONLY with valid JSON in this exact format (no markdown, no explanation):
            {
              "smartGoal": "A specific, measurable goal statement",
              "analysis": "Why this goal works for the user based on their input",
              "milestones": ["Milestone 1", "Milestone 2", "Milestone 3"],
              "tips": ["Tip 1", "Tip 2", "Tip 3"]
            }
        """.trimIndent() + "\n"

        val userPrompt = String.format(
            """
            Create a SMART fitness goal for:
            - Goal Type: %s
            - Timeframe: %s
            - User's description: %s

            Make it specific, achievable, and motivating.
            """.trimIndent() + "\n",
            goalType, timeframe, description
        )

        return generateCompletion(systemPrompt, userPrompt)
    }

    fun generateTrainingPlan(
        activityType: String,
        experienceLevel: String,
        targetDistance: Int,
        weeks: Int
    ): String {
        val systemPrompt = """
            You are a professional fitness coach AI that creates personalized training plans.
            Respond ONLY with valid JSON in this exact format (no markdown, no explanation):
            {
              "description": "Overview of the training plan",
              "weeks": [
                {
                  "week": 1,
                  "totalDistance": 40,
                  "description": "Week focus description",
                  "workouts": [
                    {
                      "day": "Mon",
                      "type": "easy run",
                      "distance": 8,
                      "duration": 45,
                      "description": "Workout description"
                    }
                  ]
                }
              ]
            }

            Workout types must be one of: "easy run", "tempo", "long run", "interval", "rest", "recovery"
            Include 4-5 workouts per week including rest days.
        """.trimIndent() + "\n"

        val userPrompt = String.format(
            """
            Create a %d-week training plan for:
            - Activity: %s
            - Experience Level: %s
            - Target Distance: %d km

            Make it progressive, realistic, and include variety.
            """.trimIndent() + "\n",
            weeks, activityType, experienceLevel, targetDistance
        )

        return generateCompletion(systemPrompt, userPrompt)
    }
}
