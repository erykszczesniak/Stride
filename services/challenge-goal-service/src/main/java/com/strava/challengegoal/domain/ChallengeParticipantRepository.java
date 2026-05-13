package com.strava.challengegoal.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChallengeParticipantRepository extends JpaRepository<ChallengeParticipant, ChallengeParticipantId> {

    List<ChallengeParticipant> findAllByChallengeId(Long challengeId);

    List<ChallengeParticipant> findAllByUserId(Long userId);

    long countByChallengeId(Long challengeId);

    boolean existsByChallengeIdAndUserId(Long challengeId, Long userId);

    void deleteByChallengeIdAndUserId(Long challengeId, Long userId);
}
