package com.strava.backend.service;

import com.strava.backend.enums.ChatRole;

import java.util.List;

/**
 * Thin wrapper over the Anthropic Claude Messages API. Kept deliberately domain-agnostic so
 * higher-level services (e.g. activity analysis) own the prompt construction and persistence.
 */
public interface ClaudeService {

    /** A single turn handed to Claude — its author and text. */
    record ClaudeMessage(ChatRole role, String content) {
    }

    /** True when an API key is configured and chat requests can be served. */
    boolean isConfigured();

    /**
     * Sends {@code conversation} (the full prior history plus the latest user turn) to Claude
     * under {@code systemPrompt} and returns the assistant's reply as plain text.
     */
    String chat(String systemPrompt, List<ClaudeMessage> conversation);
}
