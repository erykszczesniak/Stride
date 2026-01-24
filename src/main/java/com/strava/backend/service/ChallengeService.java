package com.strava.backend.service;

import com.strava.backend.domain.Challenge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ChallengeService {

    Challenge getChallengeById(Long challengeId);

    Page<Challenge> getActiveChallenges(Pageable pageable);

    Page<Challenge> getUpcomingChallenges(Pageable pageable);

    List<Challenge> getUserActiveChallenges(Long userId);

    List<Challenge> getChallengesCreatedByUser(Long userId);

    Challenge createChallenge(Challenge challenge, Long creatorId);

    Challenge updateChallenge(Long challengeId, Challenge challenge, Long userId);

    void deleteChallenge(Long challengeId, Long userId);

    void joinChallenge(Long challengeId, Long userId);

    void leaveChallenge(Long challengeId, Long userId);

    boolean isUserParticipating(Long challengeId, Long userId);

    Double getUserProgressInChallenge(Long challengeId, Long userId);

    Page<Challenge> searchChallenges(String searchText, Pageable pageable);
}
