package com.strava.challengegoal.goal;

import com.strava.challengegoal.client.ActivityClient;
import com.strava.challengegoal.client.ActivitySummary;
import com.strava.challengegoal.domain.Goal;
import com.strava.challengegoal.domain.GoalRepository;
import com.strava.challengegoal.domain.GoalType;
import com.strava.challengegoal.exception.ForbiddenException;
import com.strava.challengegoal.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goals;
    private final ActivityClient activities;

    @Transactional
    public GoalDto create(Long userId, CreateGoalRequest req) {
        Goal g = Goal.builder()
                .userId(userId)
                .title(req.title())
                .goalType(req.goalType())
                .period(req.period())
                .sportFilter(req.sportFilter())
                .targetValue(req.targetValue())
                .periodStart(req.periodStart())
                .periodEnd(req.periodEnd())
                .active(true)
                .build();
        goals.save(g);
        return toDto(g);
    }

    @Transactional(readOnly = true)
    public List<GoalDto> myActiveGoals(Long userId) {
        return goals.findAllByUserIdAndActiveTrueOrderByPeriodEndAsc(userId).stream()
                .map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public GoalDto get(Long goalId, Long requesterId) {
        Goal g = goals.findById(goalId).orElseThrow(() -> new NotFoundException("Goal " + goalId + " not found"));
        if (!g.getUserId().equals(requesterId)) {
            throw new ForbiddenException("Goals are private — you can only view your own");
        }
        return toDto(g);
    }

    @Transactional
    public void delete(Long goalId, Long userId) {
        Goal g = goals.findById(goalId).orElseThrow(() -> new NotFoundException("Goal " + goalId + " not found"));
        if (!g.getUserId().equals(userId)) {
            throw new ForbiddenException("Goals are private");
        }
        goals.delete(g);
    }

    private GoalDto toDto(Goal g) {
        ActivitySummary s = activities.summaryFor(
                g.getUserId(), g.getSportFilter(), g.getPeriodStart(), g.getPeriodEnd());
        return GoalDto.from(g, valueFor(g.getGoalType(), s));
    }

    public static double valueFor(GoalType type, ActivitySummary s) {
        return switch (type) {
            case DISTANCE       -> s.totalDistanceKm();
            case DURATION       -> s.totalDurationSeconds();
            case ELEVATION      -> s.totalElevationMeters();
            case ACTIVITY_COUNT -> s.activityCount();
        };
    }
}
