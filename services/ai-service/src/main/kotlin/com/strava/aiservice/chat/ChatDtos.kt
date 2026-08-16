package com.strava.aiservice.chat

import com.strava.aiservice.domain.ChatRole
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/** Body for sending a message into an activity's AI analysis chat (main's dto/ai/ChatRequest). */
data class ChatRequest(
    @field:NotBlank
    @field:Size(max = 2000)
    val message: String? = null
)

/** A single rendered chat turn returned to the client (main's dto/ai/ChatMessageResponse). */
data class ChatMessageResponse(
    val role: ChatRole,
    val content: String,
    val createdAt: LocalDateTime?
)

/** The full AI conversation for one activity, in chronological order (main's dto/ai/ConversationResponse). */
data class ConversationResponse(
    val activityId: Long,
    val messages: List<ChatMessageResponse>
)
