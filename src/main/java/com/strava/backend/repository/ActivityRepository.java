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

    @Query("SELECT a FROM Activity a WHERE a.user.id = :userId ORDER BY a.createdAt DESC")
    Page<ActivityProjection> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

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
}
