package com.strava.challengegoal.challenge;

import com.strava.challengegoal.domain.Challenge;
import com.strava.challengegoal.domain.ChallengeVisibility;
import com.strava.challengegoal.domain.GoalType;

import java.time.LocalDateTime;

public record ChallengeDto(
        Long id,
        String name,
        String description,
        GoalType goalType,
        String sportFilter,
        Double targetValue,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Long creatorUserId,
        ChallengeVisibility visibility,
        long participantCount,
        LocalDateTime createdAt
) {
    public static ChallengeDto from(Challenge c, long participantCount) {
        return new ChallengeDto(c.getId(), c.getName(), c.getDescription(),
                c.getGoalType(), c.getSportFilter(), c.getTargetValue(),
                c.getStartTime(), c.getEndTime(), c.getCreatorUserId(),
                c.getVisibility(), participantCount, c.getCreatedAt());
    }
}
