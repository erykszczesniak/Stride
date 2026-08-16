package com.strava.socialservice.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FollowEdgeRepository extends JpaRepository<FollowEdge, Long> {

    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    /** Recipients for fan-out when {@code followeeId} publishes an activity. */
    @Query("SELECT e.followerId FROM FollowEdge e WHERE e.followeeId = :followeeId")
    List<Long> findFollowerIdsByFolloweeId(@Param("followeeId") Long followeeId);

    @Modifying
    @Query("DELETE FROM FollowEdge e WHERE e.followerId = :followerId AND e.followeeId = :followeeId")
    int deleteByFollowerIdAndFolloweeId(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    /** Purge every edge touching a deleted user, in either direction. */
    @Modifying
    @Query("DELETE FROM FollowEdge e WHERE e.followerId = :userId OR e.followeeId = :userId")
    int deleteAllInvolving(@Param("userId") Long userId);
}
