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
public class GoalController {

    private static final Long DEFAULT_USER_ID = 1L;

    private final GoalService goalService;

    @GetMapping
    public ResponseEntity<List<GoalResponse>> getActiveGoals(
            @RequestParam(required = false) Long userId) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
        List<Goal> goals = goalService.getActiveGoalsByUserId(currentUserId);
        List<GoalResponse> response = goals.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<GoalResponse>> getAllGoals(
            @RequestParam(required = false) Long userId) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
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
    public ResponseEntity<GoalResponse> createGoal(
            @RequestParam(required = false) Long userId,
            @Valid @RequestBody GoalRequest request) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
        Goal goal = goalService.createGoal(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(goal));
    }

    @PutMapping(GOAL_ID)
    public ResponseEntity<GoalResponse> updateGoal(
            @PathVariable Long goalId,
            @RequestParam(required = false) Long userId,
            @Valid @RequestBody GoalRequest request) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
        Goal goal = goalService.updateGoal(goalId, currentUserId, request);
        return ResponseEntity.ok(mapToResponse(goal));
    }

    @DeleteMapping(GOAL_ID)
    public ResponseEntity<Void> deleteGoal(
            @PathVariable Long goalId,
            @RequestParam(required = false) Long userId) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
        goalService.deleteGoal(goalId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(PROGRESS)
    public ResponseEntity<Void> updateGoalProgress(
            @RequestParam(required = false) Long userId) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
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
