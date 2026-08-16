package com.strava.aiservice.chat

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Conversational AI analysis for a single activity, powered by Claude.
 * Public contract — byte-for-byte the monolith's AiChatController under
 * /api/v1/ai/activities/{activityId}/chat. All endpoints are scoped to the
 * authenticated user's own activities (identity via gateway X-User-* headers).
 */
@RestController
@RequestMapping("/api/v1/ai/activities/{activityId}/chat")
class AiChatController(private val aiChatService: AiChatService) {

    @GetMapping
    fun getConversation(
        @AuthenticationPrincipal userId: Long,
        @PathVariable activityId: Long
    ): ResponseEntity<ConversationResponse> =
        ResponseEntity.ok(aiChatService.getConversation(userId, activityId))

    @PostMapping
    fun sendMessage(
        @AuthenticationPrincipal userId: Long,
        @PathVariable activityId: Long,
        @Valid @RequestBody request: ChatRequest
    ): ResponseEntity<ConversationResponse> =
        ResponseEntity.ok(aiChatService.sendMessage(userId, activityId, request.message!!))

    @DeleteMapping
    fun clearConversation(
        @AuthenticationPrincipal userId: Long,
        @PathVariable activityId: Long
    ): ResponseEntity<Void> {
        aiChatService.clearConversation(userId, activityId)
        return ResponseEntity.noContent().build()
    }
}
