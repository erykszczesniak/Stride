package com.strava.backend.controller;

import com.strava.backend.dto.ai.ChatRequest;
import com.strava.backend.dto.ai.ConversationResponse;
import com.strava.backend.service.AiChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.strava.backend.constants.PathConstants.*;

/**
 * Conversational AI analysis for a single activity, powered by Claude.
 * All endpoints are scoped to the authenticated user's own activities.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(API_V1_AI + ACTIVITIES + ACTIVITY_ID)
public class AiChatController extends BaseController {

    private final AiChatService aiChatService;

    @GetMapping(CHAT)
    public ResponseEntity<ConversationResponse> getConversation(@PathVariable Long activityId) {
        return ResponseEntity.ok(aiChatService.getConversation(getAuthenticatedUserId(), activityId));
    }

    @PostMapping(CHAT)
    public ResponseEntity<ConversationResponse> sendMessage(@PathVariable Long activityId,
                                                            @Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(
                aiChatService.sendMessage(getAuthenticatedUserId(), activityId, request.getMessage()));
    }

    @DeleteMapping(CHAT)
    public ResponseEntity<Void> clearConversation(@PathVariable Long activityId) {
        aiChatService.clearConversation(getAuthenticatedUserId(), activityId);
        return ResponseEntity.noContent().build();
    }
}
