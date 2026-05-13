package com.strava.challengegoal.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    Page<Challenge> findAllByVisibilityAndEndTimeAfterOrderByStartTimeAsc(
            ChallengeVisibility visibility, LocalDateTime now, Pageable pageable);

    Page<Challenge> findAllByCreatorUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
