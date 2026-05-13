package com.strava.socialservice.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FeedItemRepository extends JpaRepository<FeedItem, Long> {

    Optional<FeedItem> findByActivityId(Long activityId);

    Page<FeedItem> findAllByOrderByStartedAtDesc(Pageable pageable);

    Page<FeedItem> findAllByUserIdOrderByStartedAtDesc(Long userId, Pageable pageable);

    @Modifying
    @Query("UPDATE FeedItem f SET f.commentCount = f.commentCount + 1 WHERE f.activityId = :activityId")
    int incrementCommentCount(@Param("activityId") Long activityId);

    @Modifying
    @Query("UPDATE FeedItem f SET f.reactionCount = f.reactionCount + 1 WHERE f.activityId = :activityId")
    int incrementReactionCount(@Param("activityId") Long activityId);
}
