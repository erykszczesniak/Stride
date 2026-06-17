package com.strava.backend.dto.ai;

import java.util.List;

/** The full AI conversation for one activity, in chronological order. */
public record ConversationResponse(Long activityId, List<ChatMessageResponse> messages) {
}
