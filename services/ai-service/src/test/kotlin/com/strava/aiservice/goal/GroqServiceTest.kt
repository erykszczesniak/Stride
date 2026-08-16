package com.strava.aiservice.goal

import com.strava.aiservice.client.ChatCompletionClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** The prompts are the product here — assert the monolith-ported wording survives intact. */
@DisplayName("GroqService")
class GroqServiceTest {

    private val chat: ChatCompletionClient = mock()
    private val service = GroqService(chat)

    @Test
    @DisplayName("generateGoal composes the monolith's SMART-goal prompts and passes the reply through")
    fun generateGoalComposesPrompts() {
        whenever(chat.complete(any(), any())).thenReturn("""{"smartGoal": "x"}""")

        val result = service.generateGoal("distance", "3months", "I want to run 5K")

        assertThat(result).isEqualTo("""{"smartGoal": "x"}""")
        val system = argumentCaptor<String>()
        val user = argumentCaptor<String>()
        verify(chat).complete(system.capture(), user.capture())

        assertThat(system.firstValue)
            .contains("SMART goals")
            .contains("Respond ONLY with valid JSON")
            .contains("\"smartGoal\"")
            .contains("\"analysis\"")
            .contains("\"milestones\"")
            .contains("\"tips\"")
        assertThat(user.firstValue)
            .contains("- Goal Type: distance")
            .contains("- Timeframe: 3months")
            .contains("- User's description: I want to run 5K")
            .contains("Make it specific, achievable, and motivating.")
    }

    @Test
    @DisplayName("generateTrainingPlan composes the monolith's plan prompts and passes the reply through")
    fun generateTrainingPlanComposesPrompts() {
        whenever(chat.complete(any(), any())).thenReturn("""{"description": "plan"}""")

        val result = service.generateTrainingPlan("cycling", "beginner", 15, 8)

        assertThat(result).isEqualTo("""{"description": "plan"}""")
        val system = argumentCaptor<String>()
        val user = argumentCaptor<String>()
        verify(chat).complete(system.capture(), user.capture())

        assertThat(system.firstValue)
            .contains("training plans")
            .contains("Respond ONLY with valid JSON")
            .contains("\"weeks\"")
            .contains("\"workouts\"")
            .contains("""Workout types must be one of: "easy run", "tempo", "long run", "interval", "rest", "recovery"""")
            .contains("Include 4-5 workouts per week including rest days.")
        assertThat(user.firstValue)
            .contains("Create a 8-week training plan for:")
            .contains("- Activity: cycling")
            .contains("- Experience Level: beginner")
            .contains("- Target Distance: 15 km")
            .contains("Make it progressive, realistic, and include variety.")
    }
}
