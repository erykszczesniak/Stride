package com.strava.socialservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Denormalized activity snapshot — one row per activity, maintained from
 * activity.created/updated/deleted/visibility-changed events. It serves three
 * jobs:
 *
 *   1. local activity-existence + owner lookup for the comment/like write
 *      side (no sync call to activity-service on writes);
 *   2. source for the public feed query and the follow-backfill;
 *   3. degraded fallback payload when activity-service is down and the feed
 *      hydration circuit is open.
 *
 * Social counts are NOT denormalized here (they were, pre-parity): they are
 * indexed COUNT(*) queries over the local comments/activity_likes tables,
 * which cannot drift under at-least-once event delivery.
 */
@Entity
@Table(name = "feed_items",
        uniqueConstraints = @UniqueConstraint(name = "uq_feed_activity", columnNames = "activity_id"),
        indexes = {
                @Index(name = "idx_feed_started",         columnList = "started_at"),
                @Index(name = "idx_feed_user_started",    columnList = "user_id, started_at"),
                @Index(name = "idx_feed_private_started", columnList = "is_private, started_at")
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

    @Column(name = "title")
    private String title;

    @Builder.Default
    @Column(name = "is_private", nullable = false)
    private boolean isPrivate = false;

    @Column(name = "distance_meters")
    private Double distanceMeters;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Column(name = "elevation_gain_meters")
    private Double elevationGainMeters;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    /** Projection of the activity's created_at — the feed sort key upstream uses. */
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
