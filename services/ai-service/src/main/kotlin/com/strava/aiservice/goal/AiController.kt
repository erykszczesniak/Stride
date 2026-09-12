package com.strava.aiservice.goal

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Public contract — byte-for-byte the monolith's AIController: loose Map
 * bodies with the same defaults, raw JSON String responses (the LLM output is
 * passed through untouched; the frontend parses it). The authenticated user
 * (gateway X-User-* headers) is handed to the service for rate limiting and
 * the audit trail; the wire contract does not change.
 */
@RestController
@RequestMapping("/api/v1/ai")
class AiController(private val groqService: GroqService) {

    @PostMapping("/generate-goal", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun generateGoal(
        @AuthenticationPrincipal userId: Long,
        @RequestBody request: Map<String, String>
    ): ResponseEntity<String> {
        val goalType = request.getOrDefault("goalType", "distance")
        val timeframe = request.getOrDefault("timeframe", "month")
        val description = request.getOrDefault("description", "")

        val result = groqService.generateGoal(userId, goalType, timeframe, description)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/generate-plan", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun generateTrainingPlan(
        @AuthenticationPrincipal userId: Long,
        @RequestBody request: Map<String, Any?>
    ): ResponseEntity<String> {
        val activityType = request.getOrDefault("activityType", "running") as String
        val experienceLevel = request.getOrDefault("experienceLevel", "intermediate") as String
        val targetDistance = request["targetDistance"]?.let { Integer.parseInt(it.toString()) } ?: 10
        val weeks = request["weeks"]?.let { Integer.parseInt(it.toString()) } ?: 4

        val result = groqService.generateTrainingPlan(userId, activityType, experienceLevel, targetDistance, weeks)
        return ResponseEntity.ok(result)
    }
}
