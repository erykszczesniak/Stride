package com.strava.backend.repository;

import com.strava.backend.domain.UserFeedEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserFeedEntryRepository extends JpaRepository<UserFeedEntry, Long> {

    /**
     * Single covering query for the feed endpoint — index-scans
     * {@code idx_user_feed_recipient_created} with no sort step.
     */
    Page<UserFeedEntry> findByRecipientUserIdOrderByActivityCreatedAtDescIdDesc(
            Long recipientUserId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM UserFeedEntry e WHERE e.activityId = :activityId")
    int deleteByActivityId(@Param("activityId") Long activityId);

    /**
     * Used when an activity flips public → private: drop every fan-out row
     * EXCEPT the author's own (the author still sees their own private activity
     * in their own feed).
     */
    @Modifying
    @Query("DELETE FROM UserFeedEntry e " +
           "WHERE e.activityId = :activityId AND e.recipientUserId <> :keepRecipientId")
    int deleteByActivityIdExceptRecipient(@Param("activityId") Long activityId,
                                          @Param("keepRecipientId") Long keepRecipientId);

    /**
     * Used on unfollow: purge everything the former-follower had inherited from
     * the no-longer-followed user. The author's own rows are untouched (they
     * point at recipient = actor, not at the unfollower).
     */
    @Modifying
    @Query("DELETE FROM UserFeedEntry e " +
           "WHERE e.recipientUserId = :recipientUserId AND e.actorUserId = :actorUserId")
    int deleteByRecipientUserIdAndActorUserId(@Param("recipientUserId") Long recipientUserId,
                                              @Param("actorUserId") Long actorUserId);

    /**
     * Idempotency helper for create / privacy-flip — lets the projector skip
     * recipients that already have a row instead of relying on the unique
     * constraint to error.
     */
    @Query("SELECT e.recipientUserId FROM UserFeedEntry e WHERE e.activityId = :activityId")
    List<Long> findRecipientIdsByActivityId(@Param("activityId") Long activityId);

    /**
     * Idempotency helper for the follow-backfill path.
     */
    @Query("SELECT e.activityId FROM UserFeedEntry e " +
           "WHERE e.recipientUserId = :recipientUserId AND e.actorUserId = :actorUserId")
    List<Long> findActivityIdsByRecipientUserIdAndActorUserId(@Param("recipientUserId") Long recipientUserId,
                                                              @Param("actorUserId") Long actorUserId);
}
