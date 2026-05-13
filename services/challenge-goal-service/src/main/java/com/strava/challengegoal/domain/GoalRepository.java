package com.strava.challengegoal.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    List<Goal> findAllByUserIdAndActiveTrueOrderByPeriodEndAsc(Long userId);

    List<Goal> findAllByUserIdAndPeriodEndAfter(Long userId, LocalDateTime now);
}
