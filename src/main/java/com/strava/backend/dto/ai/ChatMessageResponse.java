package com.strava.backend.dto.ai;

import com.strava.backend.enums.ChatRole;

import java.time.LocalDateTime;

/** A single rendered chat turn returned to the client. */
public record ChatMessageResponse(ChatRole role, String content, LocalDateTime createdAt) {
}
