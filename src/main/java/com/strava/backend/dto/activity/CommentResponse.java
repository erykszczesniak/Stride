package com.strava.backend.dto.activity;

import com.strava.backend.dto.user.BaseUserResponse;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentResponse {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private BaseUserResponse user;
}
