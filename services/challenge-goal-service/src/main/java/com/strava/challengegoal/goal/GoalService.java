package com.strava.challengegoal.goal;

import com.strava.challengegoal.client.ActivityClient;
import com.strava.challengegoal.client.ActivitySummary;
import com.strava.challengegoal.exception.ForbiddenException;
import com.strava.challengegoal.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Port of the monolith's GoalServiceImpl. Progress recomputation reads the
 * activity summary from activity-service (cached + circuit-broken) instead of
 * querying a local activities table.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoalService {

    static final String GOAL_NOT_FOUND = "Goal not found";
    static final String GOAL_ACCESS_DENIED = "You don't have permission to access this goal";

    private final GoalRepository goalRepository;
    private final ActivityClient activityClient;

    @Transactional(readOnly = true)
    public Goal getGoalById(Long goalId) {
        return goalRepository.findById(goalId)
                .orElseThrow(() -> new NotFoundException(GOAL_NOT_FOUND));
    }

    @Transactional
    public List<Goal> getActiveGoalsByUserId(Long userId) {
        List<Goal> goals = goalRepository.findActiveGoalsByUserId(userId);
        for (Goal goal : goals) {
            updateSingleGoalProgress(goal);
        }
        goalRepository.saveAll(goals);
        return goals;
    }

    @Transactional(readOnly = true)
    public List<Goal> getAllGoalsByUserId(Long userId) {
        return goalRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public Goal createGoal(Long userId, GoalRequest request) {
        LocalDateTime[] dateRange = calculateDateRange(request.getPeriod());

        Goal goal = Goal.builder()
                .goalType(request.getGoalType())
                .period(request.getPeriod())
                .activityType(request.getActivityType())
                .targetValue(request.getTargetValue())
                .currentValue(0.0)
                .startDate(dateRange[0])
                .endDate(dateRange[1])
                .isActive(true)
                .userId(userId)
                .build();

        Goal savedGoal = goalRepository.save(goal);
        updateSingleGoalProgress(savedGoal);
        return goalRepository.save(savedGoal);
    }

    @Transactional
    public Goal updateGoal(Long goalId, Long userId, GoalRequest request) {
        Goal goal = getGoalById(goalId);

        if (!goal.getUserId().equals(userId)) {
            throw new ForbiddenException(GOAL_ACCESS_DENIED);
        }

        goal.setGoalType(request.getGoalType());
        goal.setActivityType(request.getActivityType());
        goal.setTargetValue(request.getTargetValue());

        return goalRepository.save(goal);
    }

    @Transactional
    public void deleteGoal(Long goalId, Long userId) {
        Goal goal = getGoalById(goalId);

        if (!goal.getUserId().equals(userId)) {
            throw new ForbiddenException(GOAL_ACCESS_DENIED);
        }

        goalRepository.delete(goal);
    }

    @Transactional
    public void updateGoalProgress(Long userId) {
        List<Goal> activeGoals = goalRepository.findActiveGoalsByUserId(userId);
        for (Goal goal : activeGoals) {
            updateSingleGoalProgress(goal);
        }
        goalRepository.saveAll(activeGoals);
    }

    private void updateSingleGoalProgress(Goal goal) {
        ActivitySummary summary = activityClient.summaryFor(
                goal.getUserId(),
                goal.getActivityType() != null ? goal.getActivityType().name() : null,
                goal.getStartDate(),
                goal.getEndDate());

        double currentValue = switch (goal.getGoalType()) {
            case DISTANCE -> summary.totalDistanceKm();
            case TIME -> summary.totalDurationSeconds() / 3600.0;
            case ELEVATION -> summary.totalElevationMeters();
            case ACTIVITIES -> (double) summary.activityCount();
        };

        goal.setCurrentValue(currentValue);
    }

    private LocalDateTime[] calculateDateRange(GoalPeriod period) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = switch (period) {
            case WEEKLY -> now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .withHour(0).withMinute(0).withSecond(0).withNano(0);
            case MONTHLY -> now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            case YEARLY -> now.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        };
        LocalDateTime end = switch (period) {
            case WEEKLY -> start.plusWeeks(1).minusNanos(1);
            case MONTHLY -> start.plusMonths(1).minusNanos(1);
            case YEARLY -> start.plusYears(1).minusNanos(1);
        };
        return new LocalDateTime[]{start, end};
    }
}
