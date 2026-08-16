package com.strava.socialservice.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FeedItemRepository extends JpaRepository<FeedItem, Long> {

    Optional<FeedItem> findByActivityId(Long activityId);

    List<FeedItem> findAllByActivityIdIn(Collection<Long> activityIds);

    /** Public feed: non-private snapshots, newest start first. */
    Page<FeedItem> findByIsPrivateFalseOrderByStartedAtDesc(Pageable pageable);

    /**
     * Follow-backfill source — the followee's recent PUBLIC activities,
     * mirroring main's ActivityRepository.findRecentPublicByUserId
     * (ORDER BY created_at DESC, projected here as occurred_at).
     */
    @Query("SELECT f FROM FeedItem f WHERE f.userId = :userId AND f.isPrivate = false ORDER BY f.occurredAt DESC")
    List<FeedItem> findRecentPublicByUserId(@Param("userId") Long userId, Pageable pageable);

    void deleteByActivityId(Long activityId);

    void deleteByUserId(Long userId);
}
