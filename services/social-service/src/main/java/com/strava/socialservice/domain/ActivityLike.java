package com.strava.socialservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * One like per (activity, user) — mirrors main's activity_likes join table
 * (composite primary key, created_at). activity_id/user_id are logical
 * references to rows owned by activity-service/user-service.
 */
@Entity
@Table(name = "activity_likes",
        indexes = @Index(name = "idx_activity_likes_user", columnList = "user_id"))
@IdClass(ActivityLike.Key.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLike {

    @Id
    @Column(name = "activity_id")
    private Long activityId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Key implements java.io.Serializable {
        private Long activityId;
        private Long userId;
    }
}
