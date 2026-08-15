package com.strava.challengegoal.goal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/** Public contract identical to the monolith's GoalController (/api/v1/goals). */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/goals")
public class GoalController {

    private final GoalService goalService;

    @GetMapping
    public ResponseEntity<List<GoalResponse>> getActiveGoals(@AuthenticationPrincipal Long userId) {
        List<Goal> goals = goalService.getActiveGoalsByUserId(userId);
        List<GoalResponse> response = goals.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<GoalResponse>> getAllGoals(@AuthenticationPrincipal Long userId) {
        List<Goal> goals = goalService.getAllGoalsByUserId(userId);
        List<GoalResponse> response = goals.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{goalId}")
    public ResponseEntity<GoalResponse> getGoalById(@PathVariable Long goalId) {
        Goal goal = goalService.getGoalById(goalId);
        return ResponseEntity.ok(mapToResponse(goal));
    }

    @PostMapping
    public ResponseEntity<GoalResponse> createGoal(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody GoalRequest request) {
        Goal goal = goalService.createGoal(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(goal));
    }

    @PutMapping("/{goalId}")
    public ResponseEntity<GoalResponse> updateGoal(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long goalId,
            @Valid @RequestBody GoalRequest request) {
        Goal goal = goalService.updateGoal(goalId, userId, request);
        return ResponseEntity.ok(mapToResponse(goal));
    }

    @DeleteMapping("/{goalId}")
    public ResponseEntity<Void> deleteGoal(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long goalId) {
        goalService.deleteGoal(goalId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/progress")
    public ResponseEntity<Void> updateGoalProgress(@AuthenticationPrincipal Long userId) {
        goalService.updateGoalProgress(userId);
        // 200 with empty body — matches the monolith (ResponseEntity.ok().build())
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
