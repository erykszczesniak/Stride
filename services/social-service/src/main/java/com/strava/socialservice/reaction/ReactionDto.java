package com.strava.socialservice.reaction;

import com.strava.socialservice.domain.Reaction;

import java.time.LocalDateTime;

public record ReactionDto(
        Long id,
        Long activityId,
        Long userId,
        String reactionType,
        LocalDateTime createdAt
) {
    public static ReactionDto from(Reaction r) {
        return new ReactionDto(r.getId(), r.getActivityId(), r.getUserId(),
                r.getReactionType(), r.getCreatedAt());
    }
}
