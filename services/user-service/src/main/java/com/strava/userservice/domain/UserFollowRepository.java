package com.strava.userservice.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserFollowRepository extends JpaRepository<UserFollow, UserFollowId> {

    /** Users who follow the given user. */
    Page<UserFollow> findAllByUserId(Long userId, Pageable pageable);

    /** Users that the given user follows. */
    Page<UserFollow> findAllByFollowerId(Long followerId, Pageable pageable);

    /** Edges pointing at the given user — their followers (unpaged, contract returns full list). */
    List<UserFollow> findAllByUserId(Long userId);

    /** Edges originating from the given user — who they follow (unpaged). */
    List<UserFollow> findAllByFollowerId(Long followerId);

    long countByUserId(Long userId);

    long countByFollowerId(Long followerId);

    boolean existsByUserIdAndFollowerId(Long userId, Long followerId);

    void deleteByUserIdAndFollowerId(Long userId, Long followerId);

    List<UserFollow> findAllByFollowerIdAndUserIdIn(Long followerId, List<Long> userIds);
}
