package com.strava.challengegoal.goal;

import com.strava.challengegoal.domain.GoalPeriod;
import com.strava.challengegoal.domain.GoalType;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record CreateGoalRequest(
        @NotBlank @Size(max = 255) String title,
        @NotNull GoalType goalType,
        @NotNull GoalPeriod period,
        @Size(max = 32) String sportFilter,
        @NotNull @Positive Double targetValue,
        @NotNull LocalDateTime periodStart,
        @NotNull LocalDateTime periodEnd
) {}
