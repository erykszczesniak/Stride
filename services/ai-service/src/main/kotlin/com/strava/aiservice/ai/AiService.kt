package com.strava.aiservice.ai

import com.strava.aiservice.client.ChatCompletionClient
import org.springframework.stereotype.Service

/**
 * Composes domain-specific prompts and forwards them through whichever
 * ChatCompletionClient is wired in (GroqClient in prod, MockChatCompletionClient
 * in standalone).
 *
 * The split between AiService and the client is deliberate: the client owns
 * the wire format + Resilience4j wrapping, AiService owns the prompts (which
 * is where the actual product value lives). Easy to swap providers without
 * touching prompt engineering, easy to A/B prompts without touching transport.
 */
@Service
class AiService(private val chat: ChatCompletionClient) {

    private val coachSystemPrompt = """
        You are a friendly endurance-sports coach embedded inside the Stride
        fitness app. Keep responses under 80 words, conversational, focused
        on the user's data, never invent metrics that weren't provided.
    """.trimIndent()

    fun analyzeActivity(req: AnalyzeActivityRequest): AiResponse {
        val prompt = buildString {
            append("Analyze this activity: sport=${req.sport}")
            req.distanceKm?.let { append(", distance=${"%.2f".format(it)}km") }
            req.durationSeconds?.let { append(", duration=${formatTime(it)}") }
            req.elevationGainMeters?.let { append(", elevation=${it.toInt()}m") }
            req.averageHeartRate?.let { append(", avg HR=${it}bpm") }
            if (!req.notes.isNullOrBlank()) append(". Athlete notes: \"${req.notes}\"")
            append(". Give one observation and one specific suggestion for next session.")
        }
        return AiResponse(chat.complete(coachSystemPrompt, prompt))
    }

    fun trainingTip(req: TrainingTipRequest): AiResponse {
        val prompt = "Give a single training tip focused on: ${req.focus}. " +
                "One sentence, actionable, no fluff."
        return AiResponse(chat.complete(coachSystemPrompt, prompt))
    }

    fun weeklySummary(req: WeeklySummaryRequest): AiResponse {
        val breakdown = req.sportBreakdown.entries
            .joinToString(", ") { (sport, km) -> "${sport.lowercase()}=${"%.1f".format(km)}km" }
        val prompt = buildString {
            append("Summarize this week's training: ")
            append("${req.activityCount} activities, ${"%.1f".format(req.totalDistanceKm)}km total, ")
            append("${formatTime(req.totalDurationSeconds)} total time")
            if (breakdown.isNotEmpty()) append(", split: $breakdown")
            append(". Highlight a win and one thing to watch.")
        }
        return AiResponse(chat.complete(coachSystemPrompt, prompt))
    }

    private fun formatTime(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }
}
