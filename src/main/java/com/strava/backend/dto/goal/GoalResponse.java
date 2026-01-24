package com.strava.backend.dto.goal;

import com.strava.backend.enums.ActivityType;
import com.strava.backend.enums.GoalPeriod;
import com.strava.backend.enums.GoalType;
import lombok.Data;

import java.time.LocalDateTime;

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
