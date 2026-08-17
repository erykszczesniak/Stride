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

    @Query("SELECT a FROM Activity a WHERE a.userId = :userId ORDER BY a.createdAt DESC")
    Page<Activity> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(a) FROM Activity a WHERE a.userId = :userId")
    int countByUserId(@Param("userId") Long userId);

    @Query("SELECT a FROM Activity a WHERE a.userId = :userId AND a.activityType = :activityType " +
           "ORDER BY a.createdAt DESC")
    Page<Activity> findByUserIdAndActivityType(
            @Param("userId") Long userId,
            @Param("activityType") ActivityType activityType,
            Pageable pageable);

    // Search user's own activities
    @Query("SELECT a FROM Activity a WHERE a.userId = :userId AND " +
           "(UPPER(a.title) LIKE UPPER(CONCAT('%', :searchText, '%')) OR " +
           "UPPER(a.description) LIKE UPPER(CONCAT('%', :searchText, '%'))) " +
           "ORDER BY a.createdAt DESC")
    Page<Activity> searchUserActivities(@Param("userId") Long userId, @Param("searchText") String searchText, Pageable pageable);

    // Search user's own activities filtered by activity type
    @Query("SELECT a FROM Activity a WHERE a.userId = :userId AND " +
           "a.activityType = :activityType AND " +
           "(UPPER(a.title) LIKE UPPER(CONCAT('%', :searchText, '%')) OR " +
           "UPPER(a.description) LIKE UPPER(CONCAT('%', :searchText, '%'))) " +
           "ORDER BY a.createdAt DESC")
    Page<Activity> searchUserActivitiesByType(@Param("userId") Long userId, @Param("searchText") String searchText,
                                              @Param("activityType") ActivityType activityType, Pageable pageable);

    // Admin surface
    Page<Activity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT a FROM Activity a WHERE UPPER(a.title) LIKE UPPER(CONCAT('%', :search, '%')) ORDER BY a.createdAt DESC")
    Page<Activity> searchByTitle(@Param("search") String search, Pageable pageable);

    // Lifetime totals
    @Query("SELECT COALESCE(SUM(a.distance), 0) FROM Activity a WHERE a.userId = :userId")
    Double sumDistanceByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(a.duration), 0) FROM Activity a WHERE a.userId = :userId")
    Long sumDurationByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(a.elevationGain), 0) FROM Activity a WHERE a.userId = :userId")
    Long sumElevationByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(a.calories), 0) FROM Activity a WHERE a.userId = :userId")
    Long sumCaloriesByUserId(@Param("userId") Long userId);

    // Date-range aggregates (weekly trend + internal stats/range API)
    @Query("SELECT SUM(a.distance) FROM Activity a WHERE a.userId = :userId " +
           "AND a.startTime BETWEEN :startDate AND :endDate")
    Optional<Double> sumDistanceByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(a.duration) FROM Activity a WHERE a.userId = :userId " +
           "AND a.startTime BETWEEN :startDate AND :endDate")
    Optional<Long> sumDurationByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(a.elevationGain) FROM Activity a WHERE a.userId = :userId " +
           "AND a.startTime BETWEEN :startDate AND :endDate")
    Optional<Long> sumElevationByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(a.calories) FROM Activity a WHERE a.userId = :userId " +
           "AND a.startTime BETWEEN :startDate AND :endDate")
    Optional<Long> sumCaloriesByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(a) FROM Activity a WHERE a.userId = :userId " +
           "AND a.startTime BETWEEN :startDate AND :endDate")
    int countByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Daily aggregated stats for a date range (used by weekly stats)
    // EXTRACT(DOW FROM ...) returns 0=Sunday, 1=Monday, ..., 6=Saturday —
    // PostgreSQL semantics, also honored by H2 in MODE=PostgreSQL.
    @Query(value = "SELECT EXTRACT(DOW FROM a.start_time) as dow, " +
           "COALESCE(SUM(a.distance), 0.0), " +
           "COALESCE(SUM(a.duration), 0), " +
           "COUNT(a.id) " +
           "FROM activities a WHERE a.user_id = :userId " +
           "AND a.start_time BETWEEN :startDate AND :endDate " +
           "GROUP BY EXTRACT(DOW FROM a.start_time)", nativeQuery = true)
    List<Object[]> getDailyStatsByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Monthly aggregated stats (distance, duration, calories, elevation, count)
    @Query(value = "SELECT COALESCE(SUM(a.distance), 0.0), " +
           "COALESCE(SUM(a.duration), 0), " +
           "COALESCE(SUM(a.calories), 0), " +
           "COALESCE(SUM(a.elevation_gain), 0), " +
           "COUNT(a.id) " +
           "FROM activities a WHERE a.user_id = :userId " +
           "AND a.start_time BETWEEN :startDate AND :endDate", nativeQuery = true)
    Object[] getAggregatedStatsByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Window fetch for the internal user-activity-summary aggregation.
    // BETWEEN is inclusive on both ends and filters on startTime — the same
    // window rule the old client-side summation applied.
    List<Activity> findByUserIdAndStartTimeBetween(
            Long userId, LocalDateTime from, LocalDateTime to);

    // All activities for a user ordered by start time (for PR recalculation)
    @Query("SELECT a FROM Activity a WHERE a.userId = :userId ORDER BY a.startTime ASC")
    List<Activity> findAllByUserIdOrderByStartTimeAsc(@Param("userId") Long userId);
}
