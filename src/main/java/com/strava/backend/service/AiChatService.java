package com.strava.backend.service;

import com.strava.backend.dto.ai.ConversationResponse;

/**
 * Conversational AI analysis of a single activity. Owns prompt construction from the activity's
 * metrics, persistence of the conversation, and delegation to {@link ClaudeService} for replies.
 */
public interface AiChatService {

    /** Returns the stored conversation for the activity, or an empty one if none exists yet. */
    ConversationResponse getConversation(Long userId, Long activityId);

    /** Appends the user message, asks Claude to reply with full context, and persists both turns. */
    ConversationResponse sendMessage(Long userId, Long activityId, String message);

    /** Deletes the conversation (and its messages) for the activity, if any. */
    void clearConversation(Long userId, Long activityId);
}
