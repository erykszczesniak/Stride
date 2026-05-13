package com.strava.challengegoal.goal;

import com.strava.challengegoal.domain.Goal;
import com.strava.challengegoal.domain.GoalPeriod;
import com.strava.challengegoal.domain.GoalType;

import java.time.LocalDateTime;

public record GoalDto(
        Long id,
        Long userId,
        String title,
        GoalType goalType,
        GoalPeriod period,
        String sportFilter,
        Double targetValue,
        Double currentValue,
        Double progressPercent,
        LocalDateTime periodStart,
        LocalDateTime periodEnd,
        boolean active,
        LocalDateTime createdAt
) {
    public static GoalDto from(Goal g, double currentValue) {
        double pct = g.getTargetValue() > 0 ? (currentValue / g.getTargetValue()) * 100.0 : 0;
        return new GoalDto(g.getId(), g.getUserId(), g.getTitle(),
                g.getGoalType(), g.getPeriod(), g.getSportFilter(),
                g.getTargetValue(), currentValue, Math.min(100.0, pct),
                g.getPeriodStart(), g.getPeriodEnd(), g.isActive(), g.getCreatedAt());
    }
}
