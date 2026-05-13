package com.strava.socialservice.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findAllByActivityIdOrderByCreatedAtDesc(Long activityId, Pageable pageable);

    long countByActivityId(Long activityId);

    Optional<Comment> findByIdAndUserId(Long id, Long userId);
}
