package com.strava.socialservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Denormalized feed read model — one row per activity, built from
 * Kafka events (ActivityCreated → insert; CommentAdded / ReactionAdded →
 * increment counts). Activity data is denormalized so the feed query
 * never has to hit activity-service or any other DB.
 *
 * The author's displayName/avatar is NOT denormalized here — instead we
 * keep a separate user_cache table populated by UserRegisteredEvent and
 * join locally inside the read query. That way a user rename propagates
 * to every feed item without us having to fan out updates.
 */
@Entity
@Table(name = "feed_items",
        uniqueConstraints = @UniqueConstraint(name = "uq_feed_activity", columnNames = "activity_id"),
        indexes = {
                @Index(name = "idx_feed_started",      columnList = "started_at"),
                @Index(name = "idx_feed_user_started", columnList = "user_id, started_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "activity_id", nullable = false, unique = true)
    private Long activityId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "sport", nullable = false, length = 32)
    private String sport;

    @Column(name = "distance_meters")
    private Double distanceMeters;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Column(name = "elevation_gain_meters")
    private Double elevationGainMeters;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Builder.Default
    @Column(name = "comment_count", nullable = false)
    private Integer commentCount = 0;

    @Builder.Default
    @Column(name = "reaction_count", nullable = false)
    private Integer reactionCount = 0;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FeedItem f)) return false;
        return id != null && id.equals(f.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
