package com.strava.socialservice.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ActivityLikeRepository extends JpaRepository<ActivityLike, ActivityLike.Key> {

    boolean existsByActivityIdAndUserId(Long activityId, Long userId);

    long countByActivityId(Long activityId);

    /** Batch counts for the internal summary API and feed hydration. */
    @Query("SELECT l.activityId, COUNT(l) FROM ActivityLike l WHERE l.activityId IN :activityIds GROUP BY l.activityId")
    List<Object[]> countByActivityIdIn(@Param("activityIds") Collection<Long> activityIds);

    /** Which of these activities the viewer liked — one query per page. */
    @Query("SELECT l.activityId FROM ActivityLike l WHERE l.userId = :userId AND l.activityId IN :activityIds")
    List<Long> findActivityIdsLikedBy(@Param("userId") Long userId, @Param("activityIds") Collection<Long> activityIds);

    @Modifying
    @Query("DELETE FROM ActivityLike l WHERE l.activityId = :activityId AND l.userId = :userId")
    int deleteByActivityIdAndUserId(@Param("activityId") Long activityId, @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM ActivityLike l WHERE l.activityId = :activityId")
    int deleteByActivityId(@Param("activityId") Long activityId);

    @Modifying
    @Query("DELETE FROM ActivityLike l WHERE l.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
