package com.strava.backend.service;

import com.strava.backend.service.ClaudeService.ClaudeMessage;

import java.util.List;

/**
 * Groq-backed generation (goal / training plan) and the multi-turn chat
 * completion the failover path uses when Anthropic is unavailable. The
 * user-facing entry points take the caller's id so the guardrails (rate
 * limit, prompt guard, PII masking) can be applied before anything is sent.
 */
public interface GroqService {

    /** True when an API key is present. */
    boolean isConfigured();

    /** Raw single-turn completion; no guardrails — callers pass already-admitted text. */
    String generateCompletion(String systemPrompt, String userPrompt);

    String generateGoal(Long userId, String goalType, String timeframe, String description);

    String generateTrainingPlan(Long userId, String activityType, String experienceLevel,
                                int targetDistance, int weeks);

    /** Multi-turn variant for the persisted activity chat (failover from Anthropic). */
    String chatCompletion(String systemPrompt, List<ClaudeMessage> conversation);
}
