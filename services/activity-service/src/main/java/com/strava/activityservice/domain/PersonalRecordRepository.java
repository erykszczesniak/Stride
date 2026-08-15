package com.strava.activityservice.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PersonalRecordRepository extends JpaRepository<PersonalRecord, Long> {

    @Query("SELECT pr FROM PersonalRecord pr WHERE pr.userId = :userId ORDER BY pr.distanceType ASC")
    List<PersonalRecord> findByUserId(@Param("userId") Long userId);

    @Query("SELECT pr FROM PersonalRecord pr WHERE pr.userId = :userId AND pr.activityType = :activityType " +
           "ORDER BY pr.distanceType ASC")
    List<PersonalRecord> findByUserIdAndActivityType(@Param("userId") Long userId,
                                                     @Param("activityType") ActivityType activityType);

    Optional<PersonalRecord> findByUserIdAndActivityTypeAndDistanceType(
            Long userId, ActivityType activityType, DistanceType distanceType);

    /**
     * Bulk-delete every PR for a user as a single SQL statement, executed
     * immediately rather than batched into the persistence context. This is
     * what recalculateAllPRs() needs: without it, Hibernate would order the
     * subsequent INSERTs of fresh PRs BEFORE the DELETEs at flush time, and
     * the unique (user_id, activity_type, distance_type) constraint would
     * blow up on the very first new PR for the same distance.
     *
     * clearAutomatically removes the now-stale PRs from the persistence
     * context so subsequent finds in the same transaction don't return them.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM PersonalRecord pr WHERE pr.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    /**
     * Mirrors the monolith's {@code activity_id ... ON DELETE CASCADE}: when
     * an activity is deleted its PR rows disappear with it (no recompute).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM PersonalRecord pr WHERE pr.activityId = :activityId")
    void deleteByActivityId(@Param("activityId") Long activityId);
}
