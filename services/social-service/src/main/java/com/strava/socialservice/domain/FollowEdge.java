package com.strava.socialservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Local projection of the follow graph owned by user-service, maintained from
 * user.followed / user.unfollowed / user.deleted events. The feed projector
 * reads follower ids from here when fanning activities out — no synchronous
 * call to user-service on the write path.
 */
@Entity
@Table(name = "follow_edges",
        uniqueConstraints = @UniqueConstraint(name = "uq_follow_edge", columnNames = {"follower_id", "followee_id"}),
        indexes = @Index(name = "idx_follow_followee", columnList = "followee_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "follower_id", nullable = false)
    private Long followerId;

    @Column(name = "followee_id", nullable = false)
    private Long followeeId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FollowEdge f)) return false;
        return id != null && id.equals(f.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
