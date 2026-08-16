package com.strava.socialservice.comment;

import com.strava.socialservice.user.BaseUserResponse;
import lombok.Data;

import java.time.LocalDateTime;

/** Ported 1:1 from main — field names and JSON shape are the public contract. */
@Data
public class CommentResponse {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private BaseUserResponse user;
}
