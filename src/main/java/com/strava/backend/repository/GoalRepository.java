package com.strava.backend.repository;

import com.strava.backend.domain.Goal;
import com.strava.backend.enums.GoalPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {

    @Query("SELECT g FROM Goal g WHERE g.user.id = :userId AND g.isActive = true ORDER BY g.endDate ASC")
    List<Goal> findActiveGoalsByUserId(@Param("userId") Long userId);

    @Query("SELECT g FROM Goal g WHERE g.user.id = :userId ORDER BY g.createdAt DESC")
    List<Goal> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT g FROM Goal g WHERE g.user.id = :userId AND g.period = :period AND g.isActive = true " +
           "AND g.startDate <= :currentDate AND g.endDate >= :currentDate")
    Optional<Goal> findActiveGoalByUserIdAndPeriod(
            @Param("userId") Long userId,
            @Param("period") GoalPeriod period,
            @Param("currentDate") LocalDateTime currentDate);

    @Query("SELECT g FROM Goal g WHERE g.isActive = true AND g.endDate < :currentDate")
    List<Goal> findExpiredGoals(@Param("currentDate") LocalDateTime currentDate);
}
