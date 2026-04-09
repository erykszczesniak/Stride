package com.strava.backend.repository;

import com.strava.backend.domain.Activity;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.repository.projection.ActivityProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {

    Page<Activity> findByIsPrivateFalse(Pageable pageable);

    @Query("SELECT a FROM Activity a WHERE a.user.id = :userId ORDER BY a.createdAt DESC")
    Page<ActivityProjection> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT a FROM Activity a WHERE a.user.id = :userId ORDER BY a.createdAt DESC")
    Page<Activity> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT a FROM Activity a WHERE a.user.id IN :userIds ORDER BY a.createdAt DESC")
    Page<Activity> findByUserIdIn(@Param("userIds") List<Long> userIds, Pageable pageable);

    @Query("SELECT a FROM Activity a WHERE a.user.id = :userId ORDER BY a.createdAt DESC")
    List<Activity> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT a FROM Activity a WHERE a.user.id IN :userIds AND a.isPrivate = false " +
           "ORDER BY a.createdAt DESC")
    Page<ActivityProjection> findFeedActivities(@Param("userIds") List<Long> userIds, Pageable pageable);

    @Query("SELECT a FROM Activity a WHERE a.isPrivate = false ORDER BY a.createdAt DESC")
    Page<ActivityProjection> findPublicActivities(Pageable pageable);

    @Query("SELECT a FROM Activity a WHERE a.user.id = :userId " +
           "AND a.startTime BETWEEN :startDate AND :endDate")
    List<Activity> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(a.distance) FROM Activity a WHERE a.user.id = :userId " +
           "AND a.startTime BETWEEN :startDate AND :endDate")
    Optional<Double> sumDistanceByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(a.duration) FROM Activity a WHERE a.user.id = :userId " +
           "AND a.startTime BETWEEN :startDate AND :endDate")
    Optional<Long> sumDurationByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(a.calories) FROM Activity a WHERE a.user.id = :userId " +
           "AND a.startTime BETWEEN :startDate AND :endDate")
    Optional<Integer> sumCaloriesByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(a.elevationGain) FROM Activity a WHERE a.user.id = :userId " +
           "AND a.startTime BETWEEN :startDate AND :endDate")
    Optional<Integer> sumElevationByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(a) FROM Activity a WHERE a.user.id = :userId " +
           "AND a.startTime BETWEEN :startDate AND :endDate")
    int countByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(a) FROM Activity a WHERE a.user.id = :userId " +
           "AND (:activityType IS NULL OR a.activityType = :activityType) " +
           "AND a.startTime BETWEEN :startDate AND :endDate")
    int countByUserAndActivityTypeAndDateRange(
            @Param("userId") Long userId,
            @Param("activityType") ActivityType activityType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(a) FROM Activity a WHERE a.user.id = :userId")
    int countByUserId(@Param("userId") Long userId);

    @Query("SELECT a FROM Activity a WHERE a.user.id = :userId " +
           "AND (:activityType IS NULL OR a.activityType = :activityType) " +
           "AND a.startTime BETWEEN :startDate AND :endDate")
    List<Activity> findByUserIdAndActivityTypeAndDateRange(
            @Param("userId") Long userId,
            @Param("activityType") ActivityType activityType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM Activity a JOIN a.likedBy l " +
           "WHERE a.id = :activityId AND l.id = :userId")
    boolean isLikedByUser(@Param("activityId") Long activityId, @Param("userId") Long userId);

    // Challenge progress queries
    @Query("SELECT COALESCE(SUM(a.distance), 0) FROM Activity a WHERE a.user.id = :userId " +
           "AND (:activityType IS NULL OR a.activityType = :activityType) " +
           "AND a.startTime BETWEEN :startDate AND :endDate")
    Double sumDistanceForChallenge(
            @Param("userId") Long userId,
            @Param("activityType") ActivityType activityType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(a.duration), 0) FROM Activity a WHERE a.user.id = :userId " +
           "AND (:activityType IS NULL OR a.activityType = :activityType) " +
           "AND a.startTime BETWEEN :startDate AND :endDate")
    Double sumDurationForChallenge(
            @Param("userId") Long userId,
            @Param("activityType") ActivityType activityType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(a.elevationGain), 0) FROM Activity a WHERE a.user.id = :userId " +
           "AND (:activityType IS NULL OR a.activityType = :activityType) " +
           "AND a.startTime BETWEEN :startDate AND :endDate")
    Double sumElevationForChallenge(
            @Param("userId") Long userId,
            @Param("activityType") ActivityType activityType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(a) FROM Activity a WHERE a.user.id = :userId " +
           "AND (:activityType IS NULL OR a.activityType = :activityType) " +
           "AND a.startTime BETWEEN :startDate AND :endDate")
    Double countActivitiesForChallenge(
            @Param("userId") Long userId,
            @Param("activityType") ActivityType activityType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Daily aggregated stats for a date range (used by weekly stats)
    // EXTRACT(DOW FROM ...) returns 0=Sunday, 1=Monday, ..., 6=Saturday in PostgreSQL
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

    // Search activities
    @Query("SELECT a FROM Activity a WHERE a.isPrivate = false AND " +
           "(UPPER(a.title) LIKE UPPER(CONCAT('%', :searchText, '%')) OR " +
           "UPPER(a.description) LIKE UPPER(CONCAT('%', :searchText, '%'))) " +
           "ORDER BY a.createdAt DESC")
    Page<Activity> searchActivities(@Param("searchText") String searchText, Pageable pageable);

    // Get activities by type
    @Query("SELECT a FROM Activity a WHERE a.user.id = :userId AND a.activityType = :activityType " +
           "ORDER BY a.createdAt DESC")
    Page<Activity> findByUserIdAndActivityType(
            @Param("userId") Long userId,
            @Param("activityType") ActivityType activityType,
            Pageable pageable);

    @Query("SELECT COUNT(a) FROM Activity a WHERE a.createdAt >= :since")
    long countActivitiesCreatedSince(@Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(a.distance), 0) FROM Activity a")
    Double sumAllDistances();

    @Query("SELECT COUNT(a) FROM Activity a WHERE a.createdAt >= :startOfMonth")
    long countActivitiesThisMonth(@Param("startOfMonth") LocalDateTime startOfMonth);

    Page<Activity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT a FROM Activity a WHERE UPPER(a.title) LIKE UPPER(CONCAT('%', :search, '%')) ORDER BY a.createdAt DESC")
    Page<Activity> searchByTitle(@Param("search") String search, Pageable pageable);

    // Search user's own activities
    @Query("SELECT a FROM Activity a WHERE a.user.id = :userId AND " +
           "(UPPER(a.title) LIKE UPPER(CONCAT('%', :searchText, '%')) OR " +
           "UPPER(a.description) LIKE UPPER(CONCAT('%', :searchText, '%'))) " +
           "ORDER BY a.createdAt DESC")
    Page<Activity> searchUserActivities(@Param("userId") Long userId, @Param("searchText") String searchText, Pageable pageable);

    // Search user's own activities filtered by activity type
    @Query("SELECT a FROM Activity a WHERE a.user.id = :userId AND " +
           "a.activityType = :activityType AND " +
           "(UPPER(a.title) LIKE UPPER(CONCAT('%', :searchText, '%')) OR " +
           "UPPER(a.description) LIKE UPPER(CONCAT('%', :searchText, '%'))) " +
           "ORDER BY a.createdAt DESC")
    Page<Activity> searchUserActivitiesByType(@Param("userId") Long userId, @Param("searchText") String searchText,
                                              @Param("activityType") ActivityType activityType, Pageable pageable);

    // Get total distance for user
    @Query("SELECT COALESCE(SUM(a.distance), 0) FROM Activity a WHERE a.user.id = :userId")
    Double sumDistanceByUserId(@Param("userId") Long userId);

    // Get total duration for user
    @Query("SELECT COALESCE(SUM(a.duration), 0) FROM Activity a WHERE a.user.id = :userId")
    Long sumDurationByUserId(@Param("userId") Long userId);
}
