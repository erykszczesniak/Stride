package com.strava.challengegoal.challenge;

import com.strava.challengegoal.domain.ChallengeVisibility;
import com.strava.challengegoal.domain.GoalType;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record CreateChallengeRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 2000) String description,
        @NotNull GoalType goalType,
        @Size(max = 32) String sportFilter,
        @NotNull @Positive Double targetValue,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        @NotNull ChallengeVisibility visibility
) {}
