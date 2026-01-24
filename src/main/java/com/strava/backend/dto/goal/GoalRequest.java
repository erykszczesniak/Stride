package com.strava.backend.dto.goal;

import com.strava.backend.enums.ActivityType;
import com.strava.backend.enums.GoalPeriod;
import com.strava.backend.enums.GoalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

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
