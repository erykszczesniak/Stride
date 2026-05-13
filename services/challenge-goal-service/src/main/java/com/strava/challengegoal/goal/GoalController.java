package com.strava.challengegoal.goal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goals;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GoalDto create(@AuthenticationPrincipal Long userId, @Valid @RequestBody CreateGoalRequest req) {
        return goals.create(userId, req);
    }

    @GetMapping("/me")
    public List<GoalDto> mine(@AuthenticationPrincipal Long userId) {
        return goals.myActiveGoals(userId);
    }

    @GetMapping("/{id}")
    public GoalDto get(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return goals.get(id, userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        goals.delete(id, userId);
    }
}
