package com.strava.backend.service;

import com.strava.backend.domain.Goal;
import com.strava.backend.dto.goal.GoalRequest;
import com.strava.backend.enums.GoalPeriod;

import java.util.List;
import java.util.Optional;

public interface GoalService {

    Goal getGoalById(Long goalId);

    List<Goal> getActiveGoalsByUserId(Long userId);

    List<Goal> getAllGoalsByUserId(Long userId);

    Optional<Goal> getActiveGoalByPeriod(Long userId, GoalPeriod period);

    Goal createGoal(Long userId, GoalRequest request);

    Goal updateGoal(Long goalId, Long userId, GoalRequest request);

    void deleteGoal(Long goalId, Long userId);

    void updateGoalProgress(Long userId);
}
