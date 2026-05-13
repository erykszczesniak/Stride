package com.strava.socialservice.comment;

import com.strava.socialservice.domain.Comment;

import java.time.LocalDateTime;

public record CommentDto(
        Long id,
        Long activityId,
        Long userId,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CommentDto from(Comment c) {
        return new CommentDto(c.getId(), c.getActivityId(), c.getUserId(),
                c.getContent(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
