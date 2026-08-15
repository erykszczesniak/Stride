package com.strava.activityservice.activity;

import com.strava.activityservice.user.BaseUserResponse;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Ported 1:1 from main. Comment CRUD itself is owned by social-service — the
 * activity detail keeps the (always-empty here) comments field so the JSON
 * shape matches main; the frontend loads the actual thread from
 * /api/v1/activities/{id}/comments.
 */
@Data
public class CommentResponse {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private BaseUserResponse user;
}
