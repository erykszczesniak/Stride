package com.strava.userservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_followers")
@IdClass(UserFollowId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFollow {

    /** The user being followed. */
    @Id
    @Column(name = "user_id")
    private Long userId;

    /** The user doing the following. */
    @Id
    @Column(name = "follower_id")
    private Long followerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
