package com.strava.aiservice.ai

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/ai")
class AiController(private val ai: AiService) {

    @PostMapping("/activity-analysis")
    fun analyze(@Valid @RequestBody req: AnalyzeActivityRequest): AiResponse = ai.analyzeActivity(req)

    @PostMapping("/training-tip")
    fun tip(@Valid @RequestBody req: TrainingTipRequest): AiResponse = ai.trainingTip(req)

    @PostMapping("/weekly-summary")
    fun summary(@Valid @RequestBody req: WeeklySummaryRequest): AiResponse = ai.weeklySummary(req)
}
