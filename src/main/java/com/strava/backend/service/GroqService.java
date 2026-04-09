package com.strava.backend.service;

public interface GroqService {

    String generateCompletion(String systemPrompt, String userPrompt);

    String generateGoal(String goalType, String timeframe, String description);

    String generateTrainingPlan(String activityType, String experienceLevel,
                                int targetDistance, int weeks);
}
