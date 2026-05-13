package com.strava.challengegoal.challenge;

public record ChallengeProgressDto(
        Long challengeId,
        Long userId,
        Double currentValue,
        Double targetValue,
        Double progressPercent,
        boolean completed
) {}
