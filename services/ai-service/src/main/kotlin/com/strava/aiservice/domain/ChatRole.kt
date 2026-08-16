package com.strava.aiservice.domain

/**
 * Author of a message in an AI activity-analysis conversation.
 * USER — the athlete's prompt; ASSISTANT — Claude's reply.
 * Mirrors the monolith's enums/ChatRole (names are part of the public
 * contract — they appear verbatim in ConversationResponse JSON).
 */
enum class ChatRole {
    USER,
    ASSISTANT
}
