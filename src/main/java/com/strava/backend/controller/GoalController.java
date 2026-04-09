package com.strava.backend.controller;

import com.strava.backend.domain.Goal;
import com.strava.backend.dto.goal.GoalRequest;
import com.strava.backend.dto.goal.GoalResponse;
import com.strava.backend.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static com.strava.backend.constants.PathConstants.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(API_V1_GOALS)
public class GoalController extends BaseController {

    private final GoalService goalService;


    @GetMapping
    public ResponseEntity<List<GoalResponse>> getActiveGoals() {
        Long currentUserId = getAuthenticatedUserId();
        List<Goal> goals = goalService.getActiveGoalsByUserId(currentUserId);
        List<GoalResponse> response = goals.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping(ALL)
    public ResponseEntity<List<GoalResponse>> getAllGoals() {
        Long currentUserId = getAuthenticatedUserId();
        List<Goal> goals = goalService.getAllGoalsByUserId(currentUserId);
        List<GoalResponse> response = goals.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping(GOAL_ID)
    public ResponseEntity<GoalResponse> getGoalById(@PathVariable Long goalId) {
        Goal goal = goalService.getGoalById(goalId);
        return ResponseEntity.ok(mapToResponse(goal));
    }

    @PostMapping
    public ResponseEntity<GoalResponse> createGoal(@Valid @RequestBody GoalRequest request) {
        Long currentUserId = getAuthenticatedUserId();
        Goal goal = goalService.createGoal(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(goal));
    }

    @PutMapping(GOAL_ID)
    public ResponseEntity<GoalResponse> updateGoal(
            @PathVariable Long goalId,
            @Valid @RequestBody GoalRequest request) {
        Long currentUserId = getAuthenticatedUserId();
        Goal goal = goalService.updateGoal(goalId, currentUserId, request);
        return ResponseEntity.ok(mapToResponse(goal));
    }

    @DeleteMapping(GOAL_ID)
    public ResponseEntity<Void> deleteGoal(@PathVariable Long goalId) {
        Long currentUserId = getAuthenticatedUserId();
        goalService.deleteGoal(goalId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(PROGRESS)
    public ResponseEntity<Void> updateGoalProgress() {
        Long currentUserId = getAuthenticatedUserId();
        goalService.updateGoalProgress(currentUserId);
        return ResponseEntity.ok().build();
    }

    private GoalResponse mapToResponse(Goal goal) {
        GoalResponse response = new GoalResponse();
        response.setId(goal.getId());
        response.setGoalType(goal.getGoalType());
        response.setPeriod(goal.getPeriod());
        response.setActivityType(goal.getActivityType());
        response.setTargetValue(goal.getTargetValue());
        response.setCurrentValue(goal.getCurrentValue());
        response.setProgressPercentage(goal.getProgressPercentage());
        response.setRemainingValue(goal.getRemainingValue());
        response.setStartDate(goal.getStartDate());
        response.setEndDate(goal.getEndDate());
        response.setActive(goal.isActive());
        response.setCompleted(goal.isCompleted());
        return response;
    }
}
