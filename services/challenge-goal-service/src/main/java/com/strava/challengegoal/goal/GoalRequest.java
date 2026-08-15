package com.strava.challengegoal.goal;

import com.strava.challengegoal.common.ActivityType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** Verbatim copy of the monolith's GoalRequest. */
@Data
public class GoalRequest {

    @NotNull(message = "Goal type is required")
    private GoalType goalType;

    @NotNull(message = "Period is required")
    private GoalPeriod period;

    private ActivityType activityType;

    @NotNull(message = "Target value is required")
    @Positive(message = "Target value must be positive")
    private Double targetValue;
}
