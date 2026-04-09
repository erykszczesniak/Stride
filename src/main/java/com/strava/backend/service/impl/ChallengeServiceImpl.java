package com.strava.backend.service.impl;

import com.strava.backend.domain.Challenge;
import com.strava.backend.domain.User;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.repository.ChallengeRepository;
import com.strava.backend.repository.UserRepository;
import com.strava.backend.service.ChallengeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.strava.backend.constants.ErrorMessage.*;

@Service
@RequiredArgsConstructor
public class ChallengeServiceImpl implements ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;

    @Override
    @Transactional(readOnly = true)
    public Challenge getChallengeById(Long challengeId) {
        return challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ApiRequestException("Challenge not found", HttpStatus.NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Challenge> getActiveChallenges(Pageable pageable) {
        return challengeRepository.findActivePublicChallenges(LocalDateTime.now(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Challenge> getUpcomingChallenges(Pageable pageable) {
        return challengeRepository.findUpcomingPublicChallenges(LocalDateTime.now(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Challenge> getUserActiveChallenges(Long userId) {
        return challengeRepository.findActiveChallengesByUserId(userId, LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Challenge> getChallengesCreatedByUser(Long userId) {
        return challengeRepository.findByCreatorId(userId);
    }

    @Override
    @Transactional
    public Challenge createChallenge(Challenge challenge, Long creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));

        challenge.setCreator(creator);
        challenge.getParticipants().add(creator);

        return challengeRepository.save(challenge);
    }

    @Override
    @Transactional
    public Challenge updateChallenge(Long challengeId, Challenge updatedChallenge, Long userId) {
        Challenge challenge = getChallengeById(challengeId);

        if (!challenge.getCreator().getId().equals(userId)) {
            throw new ApiRequestException("Only the creator can update this challenge", HttpStatus.FORBIDDEN);
        }

        if (challenge.hasStarted()) {
            throw new ApiRequestException("Cannot update a challenge that has already started", HttpStatus.BAD_REQUEST);
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

    @Override
    @Transactional
    public void deleteChallenge(Long challengeId, Long userId) {
        Challenge challenge = getChallengeById(challengeId);

        if (!challenge.getCreator().getId().equals(userId)) {
            throw new ApiRequestException("Only the creator can delete this challenge", HttpStatus.FORBIDDEN);
        }

        challengeRepository.delete(challenge);
    }

    @Override
    @Transactional
    public void joinChallenge(Long challengeId, Long userId) {
        Challenge challenge = getChallengeById(challengeId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (challenge.hasEnded()) {
            throw new ApiRequestException("This challenge has already ended", HttpStatus.BAD_REQUEST);
        }

        if (!challenge.isPublic()) {
            throw new ApiRequestException("This challenge is private", HttpStatus.FORBIDDEN);
        }

        if (challengeRepository.isUserParticipating(challengeId, userId)) {
            throw new ApiRequestException("You are already participating in this challenge", HttpStatus.BAD_REQUEST);
        }

        challenge.getParticipants().add(user);
        challengeRepository.save(challenge);
    }

    @Override
    @Transactional
    public void leaveChallenge(Long challengeId, Long userId) {
        Challenge challenge = getChallengeById(challengeId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (challenge.getCreator().getId().equals(userId)) {
            throw new ApiRequestException("Creator cannot leave the challenge", HttpStatus.BAD_REQUEST);
        }

        if (!challengeRepository.isUserParticipating(challengeId, userId)) {
            throw new ApiRequestException("You are not participating in this challenge", HttpStatus.BAD_REQUEST);
        }

        challenge.getParticipants().remove(user);
        challengeRepository.save(challenge);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserParticipating(Long challengeId, Long userId) {
        return challengeRepository.isUserParticipating(challengeId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getUserProgressInChallenge(Long challengeId, Long userId) {
        Challenge challenge = getChallengeById(challengeId);

        // Calculate user's progress based on activities within challenge period
        Double progress = switch (challenge.getChallengeType()) {
            case DISTANCE -> activityRepository.sumDistanceForChallenge(
                    userId, challenge.getActivityType(), challenge.getStartDate(), challenge.getEndDate());
            case TIME -> activityRepository.sumDurationForChallenge(
                    userId, challenge.getActivityType(), challenge.getStartDate(), challenge.getEndDate()) / 3600.0;
            case ELEVATION -> activityRepository.sumElevationForChallenge(
                    userId, challenge.getActivityType(), challenge.getStartDate(), challenge.getEndDate());
            case ACTIVITIES_COUNT -> activityRepository.countActivitiesForChallenge(
                    userId, challenge.getActivityType(), challenge.getStartDate(), challenge.getEndDate());
            case SEGMENT -> 0.0; // Segment challenges not yet implemented
        };

        return progress != null ? progress : 0.0;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Challenge> searchChallenges(String searchText, Pageable pageable) {
        return challengeRepository.searchChallenges(searchText, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getLeaderboard(Long challengeId, Pageable pageable) {
        Challenge challenge = getChallengeById(challengeId);
        Page<User> participants = challengeRepository.findParticipantsByChallengeId(challengeId, pageable);

        return participants.getContent().stream()
                .map(user -> {
                    Double progress = getUserProgressInChallenge(challengeId, user.getId());
                    return Map.<String, Object>of(
                            "userId", user.getId(),
                            "userName", user.getFirstName() + " " + user.getLastName(),
                            "avatar", user.getAvatar() != null ? user.getAvatar() : "",
                            "progress", progress,
                            "percentage", Math.min((progress / challenge.getTargetValue()) * 100, 100)
                    );
                })
                .sorted((a, b) -> Double.compare((Double) b.get("progress"), (Double) a.get("progress")))
                .toList();
    }
}
