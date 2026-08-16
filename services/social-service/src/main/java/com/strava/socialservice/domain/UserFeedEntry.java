package com.strava.socialservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Read-model row for the activity feed, ported from main's UserFeedEntry.
 * One entry per (recipient, activity) pair. Maintained by
 * {@code UserFeedProjector} in response to Kafka events; NEVER written by
 * feature code directly. To rebuild from scratch, truncate this table and
 * re-run the backfill statements from {@code V3__sample_social_data.sql}
 * (or replay the activity/follow topics).
 */
@Entity
@Table(
        name = "user_feed_entries",
        uniqueConstraints = @UniqueConstraint(
                name = "uniq_user_feed_recipient_activity",
                columnNames = {"recipient_user_id", "activity_id"}
        ),
        indexes = {
                @Index(name = "idx_user_feed_recipient_created",
                        columnList = "recipient_user_id, activity_created_at DESC"),
                @Index(name = "idx_user_feed_recipient_actor",
                        columnList = "recipient_user_id, actor_user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFeedEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    @Column(name = "activity_id", nullable = false)
    private Long activityId;

    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    @Column(name = "activity_created_at", nullable = false)
    private LocalDateTime activityCreatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
