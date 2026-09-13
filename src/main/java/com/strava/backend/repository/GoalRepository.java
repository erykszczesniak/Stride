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

    /**
     * A user may well have several active goals for the same period (a weekly run
     * goal and a weekly ride goal, say), so this returns them ordered and the
     * caller takes the first; returning an Optional here threw
     * NonUniqueResultException and turned the dashboard into a 500.
     */
    @Query("SELECT g FROM Goal g WHERE g.user.id = :userId AND g.period = :period AND g.isActive = true " +
           "AND g.startDate <= :currentDate AND g.endDate >= :currentDate ORDER BY g.endDate ASC, g.id ASC")
    List<Goal> findActiveGoalsByUserIdAndPeriod(
            @Param("userId") Long userId,
            @Param("period") GoalPeriod period,
            @Param("currentDate") LocalDateTime currentDate);

    @Query("SELECT g FROM Goal g WHERE g.isActive = true AND g.endDate < :currentDate")
    List<Goal> findExpiredGoals(@Param("currentDate") LocalDateTime currentDate);
}
