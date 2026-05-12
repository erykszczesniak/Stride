package com.strava.activityservice.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    Page<Activity> findAllByUserIdOrderByStartTimeDesc(Long userId, Pageable pageable);

    Optional<Activity> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    List<Activity> findAllByUserIdAndStartTimeBetween(Long userId, LocalDateTime from, LocalDateTime to);

    @Query("""
        SELECT COALESCE(SUM(a.distance), 0)
        FROM Activity a
        WHERE a.userId = :userId AND a.startTime >= :since
        """)
    double sumDistanceSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
