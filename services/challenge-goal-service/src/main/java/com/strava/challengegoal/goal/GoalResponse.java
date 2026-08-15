package com.strava.challengegoal.goal;

import com.strava.challengegoal.common.ActivityType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Verbatim copy of the monolith's GoalResponse — the "isX" booleans serialize
 * as "active"/"completed" exactly like the monolith.
 */
@Data
public class GoalResponse {
    private Long id;
    private GoalType goalType;
    private GoalPeriod period;
    private ActivityType activityType;
    private Double targetValue;
    private Double currentValue;
    private Double progressPercentage;
    private Double remainingValue;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean isActive;
    private boolean isCompleted;
}
