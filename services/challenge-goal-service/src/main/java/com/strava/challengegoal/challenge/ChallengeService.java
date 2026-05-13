package com.strava.challengegoal.challenge;

import com.strava.challengegoal.client.ActivityClient;
import com.strava.challengegoal.client.ActivitySummary;
import com.strava.challengegoal.domain.*;
import com.strava.challengegoal.exception.ForbiddenException;
import com.strava.challengegoal.exception.NotFoundException;
import com.strava.challengegoal.goal.GoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final ChallengeRepository challenges;
    private final ChallengeParticipantRepository participants;
    private final ActivityClient activities;

    @Transactional
    public ChallengeDto create(Long userId, CreateChallengeRequest req) {
        Challenge c = Challenge.builder()
                .name(req.name())
                .description(req.description())
                .goalType(req.goalType())
                .sportFilter(req.sportFilter())
                .targetValue(req.targetValue())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .creatorUserId(userId)
                .visibility(req.visibility())
                .build();
        challenges.save(c);
        participants.save(ChallengeParticipant.builder()
                .challengeId(c.getId())
                .userId(userId)
                .build());
        return ChallengeDto.from(c, 1);
    }

    @Transactional(readOnly = true)
    public ChallengeDto get(Long id) {
        Challenge c = challenges.findById(id).orElseThrow(() -> new NotFoundException("Challenge " + id + " not found"));
        return ChallengeDto.from(c, participants.countByChallengeId(id));
    }

    @Transactional(readOnly = true)
    public Page<ChallengeDto> activePublic(Pageable pageable) {
        return challenges.findAllByVisibilityAndEndTimeAfterOrderByStartTimeAsc(
                ChallengeVisibility.PUBLIC, LocalDateTime.now(), pageable)
                .map(c -> ChallengeDto.from(c, participants.countByChallengeId(c.getId())));
    }

    @Transactional
    public void join(Long challengeId, Long userId) {
        if (!challenges.existsById(challengeId)) {
            throw new NotFoundException("Challenge " + challengeId + " not found");
        }
        if (participants.existsByChallengeIdAndUserId(challengeId, userId)) return;
        participants.save(ChallengeParticipant.builder()
                .challengeId(challengeId)
                .userId(userId)
                .build());
    }

    @Transactional
    public void leave(Long challengeId, Long userId) {
        participants.deleteByChallengeIdAndUserId(challengeId, userId);
    }

    @Transactional
    public void delete(Long challengeId, Long userId) {
        Challenge c = challenges.findById(challengeId).orElseThrow(() -> new NotFoundException("Challenge " + challengeId + " not found"));
        if (!c.getCreatorUserId().equals(userId)) {
            throw new ForbiddenException("Only the creator can delete the challenge");
        }
        participants.deleteAll(participants.findAllByChallengeId(challengeId));
        challenges.delete(c);
    }

    /** Compute progress for a specific participant — used both for self and leaderboard. */
    @Transactional(readOnly = true)
    public ChallengeProgressDto progress(Long challengeId, Long userId) {
        Challenge c = challenges.findById(challengeId).orElseThrow(() -> new NotFoundException("Challenge " + challengeId + " not found"));
        if (!participants.existsByChallengeIdAndUserId(challengeId, userId)) {
            throw new NotFoundException("User " + userId + " is not a participant of challenge " + challengeId);
        }
        ActivitySummary s = activities.summaryFor(userId, c.getSportFilter(), c.getStartTime(), c.getEndTime());
        double current = GoalService.valueFor(c.getGoalType(), s);
        double pct = c.getTargetValue() > 0 ? Math.min(100.0, current / c.getTargetValue() * 100.0) : 0;
        return new ChallengeProgressDto(c.getId(), userId, current, c.getTargetValue(), pct, current >= c.getTargetValue());
    }

    /** Leaderboard — sorted DESC by progress. Each participant => one call to activity-service
     *  but the Redis cache keeps repeats cheap. */
    @Transactional(readOnly = true)
    public List<ChallengeProgressDto> leaderboard(Long challengeId) {
        Challenge c = challenges.findById(challengeId).orElseThrow(() -> new NotFoundException("Challenge " + challengeId + " not found"));
        return participants.findAllByChallengeId(challengeId).stream()
                .map(p -> {
                    ActivitySummary s = activities.summaryFor(p.getUserId(), c.getSportFilter(), c.getStartTime(), c.getEndTime());
                    double cur = GoalService.valueFor(c.getGoalType(), s);
                    double pct = c.getTargetValue() > 0 ? Math.min(100.0, cur / c.getTargetValue() * 100.0) : 0;
                    return new ChallengeProgressDto(c.getId(), p.getUserId(), cur, c.getTargetValue(), pct, cur >= c.getTargetValue());
                })
                .sorted(Comparator.comparingDouble(ChallengeProgressDto::currentValue).reversed())
                .toList();
    }
}
