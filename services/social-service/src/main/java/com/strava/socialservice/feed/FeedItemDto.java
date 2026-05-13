package com.strava.socialservice.feed;

import com.strava.socialservice.domain.FeedItem;

import java.time.LocalDateTime;

public record FeedItemDto(
        Long activityId,
        Long userId,
        String userDisplayName,
        String userAvatar,
        String sport,
        Double distanceMeters,
        Long durationSeconds,
        Double elevationGainMeters,
        LocalDateTime startedAt,
        int commentCount,
        int reactionCount
) {
    public static FeedItemDto from(FeedItem f, String displayName, String avatar) {
        return new FeedItemDto(
                f.getActivityId(), f.getUserId(),
                displayName, avatar,
                f.getSport(),
                f.getDistanceMeters(), f.getDurationSeconds(), f.getElevationGainMeters(),
                f.getStartedAt(),
                f.getCommentCount(), f.getReactionCount()
        );
    }
}
