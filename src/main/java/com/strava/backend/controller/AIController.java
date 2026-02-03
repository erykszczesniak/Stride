package com.strava.backend.controller;

import com.strava.backend.service.GroqService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.strava.backend.constants.PathConstants.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(API_V1_AI)
public class AIController {

    private final GroqService groqService;

    @PostMapping(value = GENERATE_GOAL, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> generateGoal(@RequestBody Map<String, String> request) {
        String goalType = request.getOrDefault("goalType", "distance");
        String timeframe = request.getOrDefault("timeframe", "month");
        String description = request.getOrDefault("description", "");

        String result = groqService.generateGoal(goalType, timeframe, description);
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = GENERATE_PLAN, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> generateTrainingPlan(@RequestBody Map<String, Object> request) {
        String activityType = (String) request.getOrDefault("activityType", "running");
        String experienceLevel = (String) request.getOrDefault("experienceLevel", "intermediate");
        int targetDistance = request.get("targetDistance") != null
            ? Integer.parseInt(request.get("targetDistance").toString())
            : 10;
        int weeks = request.get("weeks") != null
            ? Integer.parseInt(request.get("weeks").toString())
            : 4;

        String result = groqService.generateTrainingPlan(activityType, experienceLevel, targetDistance, weeks);
        return ResponseEntity.ok(result);
    }
}
