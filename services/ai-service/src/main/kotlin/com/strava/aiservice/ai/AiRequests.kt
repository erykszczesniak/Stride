package com.strava.aiservice.ai

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class AnalyzeActivityRequest(
    @field:NotBlank
    val sport: String,
    val distanceKm: Double?,
    val durationSeconds: Long?,
    val elevationGainMeters: Double?,
    val averageHeartRate: Int?,
    @field:Size(max = 500)
    val notes: String? = null
)

data class TrainingTipRequest(
    @field:NotBlank @field:Size(max = 64)
    val focus: String   // e.g. "running endurance", "cycling recovery"
)

data class WeeklySummaryRequest(
    val totalDistanceKm: Double,
    val totalDurationSeconds: Long,
    val activityCount: Int,
    val sportBreakdown: Map<String, Double> = emptyMap()  // sport -> km
)

data class AiResponse(val content: String)
