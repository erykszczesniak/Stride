package com.strava.challengegoal.challenge;

import com.strava.challengegoal.client.ActivityClient;
import com.strava.challengegoal.client.ActivitySummary;
import com.strava.challengegoal.client.UserBatchClient;
import com.strava.challengegoal.client.UserSummary;
import com.strava.challengegoal.exception.BadRequestException;
import com.strava.challengegoal.exception.ForbiddenException;
import com.strava.challengegoal.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Port of the monolith's ChallengeServiceImpl. Progress is computed from
 * activity-service data (ActivityClient: Redis cache + Resilience4j) instead
 * of a local activities table; leaderboard names come from user-service via
 * UserBatchClient.
 */
@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final ActivityClient activityClient;
    private final UserBatchClient userBatchClient;

    @Transactional(readOnly = true)
    public Challenge getChallengeById(Long challengeId) {
        return challengeRepository.findById(challengeId)
                .orElseThrow(() -> new NotFoundException("Challenge not found"));
    }

    @Transactional(readOnly = true)
    public Page<Challenge> getActiveChallenges(Pageable pageable) {
        return challengeRepository.findActivePublicChallenges(LocalDateTime.now(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Challenge> getUpcomingChallenges(Pageable pageable) {
        return challengeRepository.findUpcomingPublicChallenges(LocalDateTime.now(), pageable);
    }

    @Transactional(readOnly = true)
    public List<Challenge> getUserActiveChallenges(Long userId) {
        return challengeRepository.findActiveChallengesByUserId(userId, LocalDateTime.now());
    }

    @Transactional
    public Challenge createChallenge(Challenge challenge, Long creatorId) {
        challenge.setCreatorId(creatorId);
        challenge.getParticipantIds().add(creatorId);
        return challengeRepository.save(challenge);
    }

    @Transactional
    public Challenge updateChallenge(Long challengeId, Challenge updatedChallenge, Long userId) {
        Challenge challenge = getChallengeById(challengeId);

        if (!challenge.getCreatorId().equals(userId)) {
            throw new ForbiddenException("Only the creator can update this challenge");
        }

        if (challenge.hasStarted()) {
            throw new BadRequestException("Cannot update a challenge that has already started");
        }

        challenge.setName(updatedChallenge.getName());
        challenge.setDescription(updatedChallenge.getDescription());
        challenge.setChallengeType(updatedChallenge.getChallengeType());
        challenge.setActivityType(updatedChallenge.getActivityType());
        challenge.setTargetValue(updatedChallenge.getTargetValue());
        challenge.setStartDate(updatedChallenge.getStartDate());
        challenge.setEndDate(updatedChallenge.getEndDate());
        challenge.setPublic(updatedChallenge.isPublic());

        if (updatedChallenge.getCoverImage() != null) {
            challenge.setCoverImage(updatedChallenge.getCoverImage());
        }

        return challengeRepository.save(challenge);
    }

    @Transactional
    public void deleteChallenge(Long challengeId, Long userId) {
        Challenge challenge = getChallengeById(challengeId);

        if (!challenge.getCreatorId().equals(userId)) {
            throw new ForbiddenException("Only the creator can delete this challenge");
        }

        challengeRepository.delete(challenge);
    }

    @Transactional
    public void joinChallenge(Long challengeId, Long userId) {
        Challenge challenge = getChallengeById(challengeId);

        if (challenge.hasEnded()) {
            throw new BadRequestException("This challenge has already ended");
        }

        if (!challenge.isPublic()) {
            throw new ForbiddenException("This challenge is private");
        }

        if (challengeRepository.isUserParticipating(challengeId, userId)) {
            throw new BadRequestException("You are already participating in this challenge");
        }

        challenge.getParticipantIds().add(userId);
        challengeRepository.save(challenge);
    }

    @Transactional
    public void leaveChallenge(Long challengeId, Long userId) {
        Challenge challenge = getChallengeById(challengeId);

        if (challenge.getCreatorId().equals(userId)) {
            throw new BadRequestException("Creator cannot leave the challenge");
        }

        if (!challengeRepository.isUserParticipating(challengeId, userId)) {
            throw new BadRequestException("You are not participating in this challenge");
        }

        challenge.getParticipantIds().remove(userId);
        challengeRepository.save(challenge);
    }

    @Transactional(readOnly = true)
    public boolean isUserParticipating(Long challengeId, Long userId) {
        return challengeRepository.isUserParticipating(challengeId, userId);
    }

    @Transactional(readOnly = true)
    public Double getUserProgressInChallenge(Long challengeId, Long userId) {
        return progressFor(getChallengeById(challengeId), userId);
    }

    @Transactional(readOnly = true)
    public Page<Challenge> searchChallenges(String searchText, Pageable pageable) {
        return challengeRepository.searchChallenges(searchText, pageable);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getLeaderboard(Long challengeId, Pageable pageable) {
        Challenge challenge = getChallengeById(challengeId);
        Page<Long> participants = challengeRepository.findParticipantIdsByChallengeId(challengeId, pageable);
        Map<Long, UserSummary> users = userBatchClient.usersByIds(participants.getContent());

        return participants.getContent().stream()
                .map(userId -> {
                    Double progress = progressFor(challenge, userId);
                    UserSummary user = users.get(userId);
                    return Map.<String, Object>of(
                            "userId", userId,
                            "userName", user != null ? user.displayName() : "User #" + userId,
                            "avatar", user != null && user.avatar() != null ? user.avatar() : "",
                            "progress", progress,
                            "percentage", Math.min((progress / challenge.getTargetValue()) * 100, 100)
                    );
                })
                .sorted((a, b) -> Double.compare((Double) b.get("progress"), (Double) a.get("progress")))
                .toList();
    }

    /**
     * Same progress semantics as the monolith's activity aggregation queries,
     * computed from the cached activity summary for the challenge window.
     */
    private Double progressFor(Challenge challenge, Long userId) {
        ActivitySummary summary = activityClient.summaryFor(
                userId,
                challenge.getActivityType() != null ? challenge.getActivityType().name() : null,
                challenge.getStartDate(),
                challenge.getEndDate());

        return switch (challenge.getChallengeType()) {
            case DISTANCE -> summary.totalDistanceKm();
            case TIME -> summary.totalDurationSeconds() / 3600.0;
            case ELEVATION -> summary.totalElevationMeters();
            case ACTIVITIES_COUNT -> (double) summary.activityCount();
            case SEGMENT -> 0.0; // Segment challenges not yet implemented
        };
    }
}
