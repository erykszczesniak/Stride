package com.strava.socialservice.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByActivityIdOrderByCreatedAtDesc(Long activityId);

    long countByActivityId(Long activityId);

    /** Batch counts for the internal summary API and feed hydration — one query per page, never per row. */
    @Query("SELECT c.activityId, COUNT(c) FROM Comment c WHERE c.activityId IN :activityIds GROUP BY c.activityId")
    List<Object[]> countByActivityIdIn(@Param("activityIds") Collection<Long> activityIds);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.activityId = :activityId")
    int deleteByActivityId(@Param("activityId") Long activityId);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
