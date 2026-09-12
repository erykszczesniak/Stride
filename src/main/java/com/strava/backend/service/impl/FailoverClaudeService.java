package com.strava.backend.service.impl;

import com.strava.backend.ai.guard.AiGuardProperties;
import com.strava.backend.ai.telemetry.AiCallRecorder;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.service.ClaudeService;
import com.strava.backend.service.GroqService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The {@link ClaudeService} the chat actually uses: Anthropic first, Groq
 * when Anthropic is unavailable. "Unavailable" means a provider-side failure
 * (502 from the Anthropic client or no key configured) — never a caller
 * error, which is rethrown untouched.
 *
 * <p>The secondary answer is a degraded one (a different model, no Claude
 * system-prompt tuning) but it keeps the coach responding during a provider
 * incident. Every failover is counted ({@code ai_failover_total}) and shows
 * up on the cost dashboard as Groq spend on the chat endpoint.
 */
@Slf4j
@Primary
@Service
public class FailoverClaudeService implements ClaudeService {

    private final ClaudeServiceImpl primary;
    private final GroqService secondary;
    private final AiGuardProperties properties;
    private final AiCallRecorder recorder;

    public FailoverClaudeService(ClaudeServiceImpl primary, GroqService secondary,
                                 AiGuardProperties properties, AiCallRecorder recorder) {
        this.primary = primary;
        this.secondary = secondary;
        this.properties = properties;
        this.recorder = recorder;
    }

    private boolean secondaryAvailable() {
        return properties.getFailover().isEnabled() && secondary.isConfigured();
    }

    @Override
    public boolean isConfigured() {
        return primary.isConfigured() || secondaryAvailable();
    }

    @Override
    public String chat(String systemPrompt, List<ClaudeMessage> conversation) {
        if (primary.isConfigured()) {
            try {
                return primary.chat(systemPrompt, conversation);
            } catch (RuntimeException e) {
                if (!shouldFailOver(e) || !secondaryAvailable()) throw e;
                log.warn("Anthropic chat unavailable ({}); failing over to Groq", describe(e));
            }
        } else if (!secondaryAvailable()) {
            throw new ApiRequestException("AI chat is not configured", HttpStatus.SERVICE_UNAVAILABLE);
        }

        recorder.recordFailover(AiCallRecorder.Endpoint.CHAT, GroqServiceImpl.PROVIDER);
        try {
            return secondary.chatCompletion(systemPrompt, conversation);
        } catch (RuntimeException e) {
            log.error("Groq failover chat failed too: {}", describe(e));
            throw new ApiRequestException("Failed to generate AI response", HttpStatus.BAD_GATEWAY);
        }
    }

    /** Provider trouble fails over; anything that is the caller's problem does not. */
    private static boolean shouldFailOver(RuntimeException e) {
        return e instanceof ApiRequestException api
                && (api.getStatus() == HttpStatus.BAD_GATEWAY || api.getStatus() == HttpStatus.SERVICE_UNAVAILABLE);
    }

    private static String describe(RuntimeException e) {
        return e instanceof ApiRequestException api
                ? api.getStatus().value() + " " + api.getMessage()
                : e.getClass().getSimpleName();
    }
}
