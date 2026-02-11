package com.strava.backend.service.impl;

import com.strava.backend.domain.Goal;
import com.strava.backend.domain.User;
import com.strava.backend.dto.goal.GoalRequest;
import com.strava.backend.enums.GoalPeriod;
import com.strava.backend.enums.GoalType;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.repository.GoalRepository;
import com.strava.backend.service.GoalService;
import com.strava.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import static com.strava.backend.constants.ErrorMessage.GOAL_ACCESS_DENIED;
import static com.strava.backend.constants.ErrorMessage.GOAL_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoalServiceImpl implements GoalService {

    private final GoalRepository goalRepository;
    private final ActivityRepository activityRepository;
    private final UserService userService;

    @Override
    public Goal getGoalById(Long goalId) {
        return goalRepository.findById(goalId)
                .orElseThrow(() -> new ApiRequestException(GOAL_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    @Override
    @Cacheable(value = "goals", key = "'active-' + #userId")
    public List<Goal> getActiveGoalsByUserId(Long userId) {
        log.debug("Cache miss - fetching active goals for user: {}", userId);
        return goalRepository.findActiveGoalsByUserId(userId);
    }

    @Override
    public List<Goal> getAllGoalsByUserId(Long userId) {
        return goalRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public Optional<Goal> getActiveGoalByPeriod(Long userId, GoalPeriod period) {
        return goalRepository.findActiveGoalByUserIdAndPeriod(userId, period, LocalDateTime.now());
    }

    @Override
    @Transactional
    @CacheEvict(value = "goals", key = "'active-' + #userId")
    public Goal createGoal(Long userId, GoalRequest request) {
        User user = userService.getUserById(userId);

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
                .user(user)
                .build();

        Goal savedGoal = goalRepository.save(goal);
        updateSingleGoalProgress(savedGoal);
        return goalRepository.save(savedGoal);
    }

    @Override
    @Transactional
    @CacheEvict(value = "goals", key = "'active-' + #userId")
    public Goal updateGoal(Long goalId, Long userId, GoalRequest request) {
        Goal goal = getGoalById(goalId);

        if (!goal.getUser().getId().equals(userId)) {
            throw new ApiRequestException(GOAL_ACCESS_DENIED, HttpStatus.FORBIDDEN);
        }

        goal.setGoalType(request.getGoalType());
        goal.setActivityType(request.getActivityType());
        goal.setTargetValue(request.getTargetValue());

        return goalRepository.save(goal);
    }

    @Override
    @Transactional
    @CacheEvict(value = "goals", key = "'active-' + #userId")
    public void deleteGoal(Long goalId, Long userId) {
        Goal goal = getGoalById(goalId);

        if (!goal.getUser().getId().equals(userId)) {
            throw new ApiRequestException(GOAL_ACCESS_DENIED, HttpStatus.FORBIDDEN);
        }

        goalRepository.delete(goal);
    }

    @Override
    @Transactional
    @CacheEvict(value = "goals", key = "'active-' + #userId")
    public void updateGoalProgress(Long userId) {
        List<Goal> activeGoals = goalRepository.findActiveGoalsByUserId(userId);
        for (Goal goal : activeGoals) {
            updateSingleGoalProgress(goal);
        }
        goalRepository.saveAll(activeGoals);
    }

    private void updateSingleGoalProgress(Goal goal) {
        Long userId = goal.getUser().getId();
        LocalDateTime startDate = goal.getStartDate();
        LocalDateTime endDate = goal.getEndDate();

        double currentValue = 0.0;

        if (goal.getGoalType() == GoalType.DISTANCE) {
            currentValue = activityRepository.sumDistanceByUserAndDateRange(userId, startDate, endDate)
                    .orElse(0.0);
        } else if (goal.getGoalType() == GoalType.TIME) {
            Long seconds = activityRepository.sumDurationByUserAndDateRange(userId, startDate, endDate)
                    .orElse(0L);
            currentValue = seconds / 3600.0; // Convert to hours
        } else if (goal.getGoalType() == GoalType.ELEVATION) {
            currentValue = activityRepository.sumElevationByUserAndDateRange(userId, startDate, endDate)
                    .orElse(0).doubleValue();
        } else if (goal.getGoalType() == GoalType.ACTIVITIES) {
            currentValue = activityRepository.countByUserAndDateRange(userId, startDate, endDate);
        }

        goal.setCurrentValue(currentValue);
    }

    private LocalDateTime[] calculateDateRange(GoalPeriod period) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start;
        LocalDateTime end;

        switch (period) {
            case WEEKLY:
                start = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        .withHour(0).withMinute(0).withSecond(0).withNano(0);
                end = start.plusWeeks(1).minusNanos(1);
                break;
            case MONTHLY:
                start = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                end = start.plusMonths(1).minusNanos(1);
                break;
            case YEARLY:
                start = now.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                end = start.plusYears(1).minusNanos(1);
                break;
            default:
                throw new IllegalArgumentException("Unknown period: " + period);
        }

        return new LocalDateTime[]{start, end};
    }
}
