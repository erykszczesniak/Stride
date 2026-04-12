package com.strava.backend.graphql;

import com.strava.backend.domain.Goal;
import com.strava.backend.dto.goal.GoalRequest;
import com.strava.backend.enums.GoalPeriod;
import com.strava.backend.controller.BaseController;
import com.strava.backend.service.GoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class GoalGraphQLController extends BaseController {

    private final GoalService goalService;

    @QueryMapping
    public List<Goal> goals(@Argument Long userId) {
        return goalService.getAllGoalsByUserId(userId);
    }

    @QueryMapping
    public List<Goal> activeGoals(@Argument Long userId) {
        return goalService.getActiveGoalsByUserId(userId);
    }

    @MutationMapping
    public Goal createGoal(@Argument("input") GoalRequest input) {
        return goalService.createGoal(getAuthenticatedUserId(), input);
    }

    @MutationMapping
    public Goal updateGoal(@Argument Long id, @Argument("input") GoalRequest input) {
        return goalService.updateGoal(id, getAuthenticatedUserId(), input);
    }

    @MutationMapping
    public Boolean deleteGoal(@Argument Long id) {
        goalService.deleteGoal(id, getAuthenticatedUserId());
        return true;
    }

    // Schema mappings for computed fields
    @SchemaMapping(typeName = "Goal", field = "progressPercentage")
    public Double progressPercentage(Goal goal) {
        if (goal.getTargetValue() == null || goal.getTargetValue() == 0) {
            return 0.0;
        }
        double current = goal.getCurrentValue() != null ? goal.getCurrentValue() : 0.0;
        return Math.min((current / goal.getTargetValue()) * 100, 100.0);
    }

    @SchemaMapping(typeName = "Goal", field = "remainingValue")
    public Double remainingValue(Goal goal) {
        double current = goal.getCurrentValue() != null ? goal.getCurrentValue() : 0.0;
        double target = goal.getTargetValue() != null ? goal.getTargetValue() : 0.0;
        return Math.max(target - current, 0.0);
    }

    @SchemaMapping(typeName = "Goal", field = "isActive")
    public Boolean isActive(Goal goal) {
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(goal.getStartDate()) && !now.isAfter(goal.getEndDate());
    }

    @SchemaMapping(typeName = "Goal", field = "isCompleted")
    public Boolean isCompleted(Goal goal) {
        if (goal.getTargetValue() == null || goal.getTargetValue() == 0) {
            return false;
        }
        double current = goal.getCurrentValue() != null ? goal.getCurrentValue() : 0.0;
        return current >= goal.getTargetValue();
    }
}
