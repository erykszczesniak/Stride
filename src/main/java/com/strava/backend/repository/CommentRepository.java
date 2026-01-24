package com.strava.backend.repository;

import com.strava.backend.domain.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c WHERE c.activity.id = :activityId ORDER BY c.createdAt DESC")
    List<Comment> findByActivityIdOrderByCreatedAtDesc(@Param("activityId") Long activityId);

    @Query("SELECT c FROM Comment c WHERE c.activity.id = :activityId ORDER BY c.createdAt DESC")
    Page<Comment> findByActivityIdOrderByCreatedAtDesc(@Param("activityId") Long activityId, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.activity.id = :activityId")
    int countByActivityId(@Param("activityId") Long activityId);
}
