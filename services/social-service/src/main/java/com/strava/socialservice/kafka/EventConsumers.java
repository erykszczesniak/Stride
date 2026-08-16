package com.strava.socialservice.kafka;

import com.strava.shared.events.ActivityCreatedEvent;
import com.strava.shared.events.ActivityDeletedEvent;
import com.strava.shared.events.ActivityVisibilityChangedEvent;
import com.strava.shared.events.Topics;
import com.strava.shared.events.UserDeletedEvent;
import com.strava.shared.events.UserFollowedEvent;
import com.strava.shared.events.UserRegisteredEvent;
import com.strava.shared.events.UserUnfollowedEvent;
import com.strava.shared.events.UserUpdatedEvent;
import com.strava.socialservice.domain.ActivityLikeRepository;
import com.strava.socialservice.domain.CommentReactionRepository;
import com.strava.socialservice.domain.CommentRepository;
import com.strava.socialservice.domain.FeedItem;
import com.strava.socialservice.domain.FeedItemRepository;
import com.strava.socialservice.domain.FollowEdge;
import com.strava.socialservice.domain.FollowEdgeRepository;
import com.strava.socialservice.domain.UserCache;
import com.strava.socialservice.domain.UserCacheRepository;
import com.strava.socialservice.domain.UserFeedEntryRepository;
import com.strava.socialservice.projection.UserFeedProjector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Read-side projections driven by domain events from user-service and
 * activity-service:
 *
 *   * user_cache          — author identity snapshots
 *   * feed_items          — activity snapshots (incl. title + privacy)
 *   * follow_edges        — the follow graph
 *   * user_feed_entries   — per-user feed fan-out (delegated to UserFeedProjector)
 *
 * All consumers share the "social-service" group and are idempotent: upserts
 * keyed by natural id, and the projector subtracts existing rows before
 * inserting. The pre-parity "social-service-counts" group (denormalized
 * comment/reaction counters) is gone — counts are now computed from the local
 * comments/activity_likes tables, which redelivery cannot corrupt.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventConsumers {

    private static final String GROUP = "social-service";

    private final FeedItemRepository feedItems;
    private final UserCacheRepository users;
    private final FollowEdgeRepository followEdges;
    private final UserFeedEntryRepository feedEntries;
    private final CommentRepository comments;
    private final CommentReactionRepository commentReactions;
    private final ActivityLikeRepository likes;
    private final UserFeedProjector projector;

    // ---------------------------------------------------------------- users

    @KafkaListener(topics = Topics.USER_REGISTERED, groupId = GROUP)
    @Transactional
    public void onUserRegistered(UserRegisteredEvent event) {
        log.debug("Caching user userId={} displayName={}", event.userId(), event.displayName());
        upsertUser(event.userId(), event.displayName(), event.avatar());
    }

    @KafkaListener(topics = Topics.USER_UPDATED, groupId = GROUP)
    @Transactional
    public void onUserUpdated(UserUpdatedEvent event) {
        log.debug("Refreshing cached user userId={} displayName={}", event.userId(), event.displayName());
        upsertUser(event.userId(), event.displayName(), event.avatar());
    }

    @KafkaListener(topics = Topics.USER_DELETED, groupId = GROUP)
    @Transactional
    public void onUserDeleted(UserDeletedEvent event) {
        Long userId = event.userId();
        log.debug("Purging social data for deleted userId={}", userId);
        users.deleteById(userId);
        followEdges.deleteAllInvolving(userId);
        feedEntries.deleteAllInvolving(userId);
        // Their content goes too — main cascaded comments/likes via user FKs.
        comments.deleteByUserId(userId);
        commentReactions.deleteByUserId(userId);
        likes.deleteByUserId(userId);
        feedItems.deleteByUserId(userId);
    }

    // ----------------------------------------------------------- activities

    @KafkaListener(topics = Topics.ACTIVITY_CREATED, groupId = GROUP)
    @Transactional
    public void onActivityCreated(ActivityCreatedEvent event) {
        log.debug("Projecting created activity activityId={} userId={}", event.activityId(), event.userId());
        upsertFeedItem(event);
        projector.onActivityCreated(event.activityId(), event.userId(), event.isPrivate(),
                toLocal(event.occurredAt()));
    }

    /** Refresh the snapshot on edits (sport/distance/duration/title/privacy). */
    @KafkaListener(topics = Topics.ACTIVITY_UPDATED, groupId = GROUP)
    @Transactional
    public void onActivityUpdated(ActivityCreatedEvent event) {
        log.debug("Refreshing snapshot for updated activityId={}", event.activityId());
        upsertFeedItem(event);
    }

    @KafkaListener(topics = Topics.ACTIVITY_DELETED, groupId = GROUP)
    @Transactional
    public void onActivityDeleted(ActivityDeletedEvent event) {
        log.debug("Purging social data for deleted activityId={}", event.activityId());
        projector.onActivityDeleted(event.activityId());
        feedItems.deleteByActivityId(event.activityId());
        // Local comments/likes go too — main cascaded them via the activity FK.
        // comment_reactions cascade off the comments FK inside this schema.
        comments.deleteByActivityId(event.activityId());
        likes.deleteByActivityId(event.activityId());
    }

    @KafkaListener(topics = Topics.ACTIVITY_VISIBILITY_CHANGED, groupId = GROUP)
    @Transactional
    public void onActivityVisibilityChanged(ActivityVisibilityChangedEvent event) {
        log.debug("Visibility change activityId={} nowPrivate={}", event.activityId(), event.nowPrivate());
        feedItems.findByActivityId(event.activityId())
                .ifPresent(item -> item.setPrivate(event.nowPrivate()));
        projector.onActivityVisibilityChanged(event.activityId(), event.userId(), event.nowPrivate(),
                activityCreatedAt(event));
    }

    // --------------------------------------------------------- follow graph

    @KafkaListener(topics = Topics.USER_FOLLOWED, groupId = GROUP)
    @Transactional
    public void onUserFollowed(UserFollowedEvent event) {
        log.debug("Follow edge {} -> {}", event.followerUserId(), event.followeeUserId());
        if (!followEdges.existsByFollowerIdAndFolloweeId(event.followerUserId(), event.followeeUserId())) {
            followEdges.save(FollowEdge.builder()
                    .followerId(event.followerUserId())
                    .followeeId(event.followeeUserId())
                    .build());
        }
        projector.onUserFollowed(event.followerUserId(), event.followeeUserId());
    }

    @KafkaListener(topics = Topics.USER_UNFOLLOWED, groupId = GROUP)
    @Transactional
    public void onUserUnfollowed(UserUnfollowedEvent event) {
        log.debug("Unfollow {} -/-> {}", event.followerUserId(), event.followeeUserId());
        followEdges.deleteByFollowerIdAndFolloweeId(event.followerUserId(), event.followeeUserId());
        projector.onUserUnfollowed(event.followerUserId(), event.followeeUserId());
    }

    // -------------------------------------------------------------- helpers

    private void upsertUser(Long userId, String displayName, String avatar) {
        String[] names = splitDisplayName(displayName);
        users.save(UserCache.builder()
                .userId(userId)
                .firstName(names[0])
                .lastName(names[1])
                .avatar(avatar)
                .updatedAt(LocalDateTime.now())
                .build());
    }

    private void upsertFeedItem(ActivityCreatedEvent event) {
        FeedItem item = feedItems.findByActivityId(event.activityId())
                .orElseGet(() -> FeedItem.builder()
                        .activityId(event.activityId())
                        .occurredAt(toLocal(event.occurredAt()))
                        .build());
        item.setUserId(event.userId());
        item.setSport(event.sport());
        item.setTitle(event.title());
        item.setPrivate(event.isPrivate());
        item.setDistanceMeters(event.distanceMeters());
        item.setDurationSeconds(event.durationSeconds());
        item.setElevationGainMeters(event.elevationGainMeters());
        item.setStartedAt(event.startedAt() != null ? toLocal(event.startedAt()) : LocalDateTime.now());
        feedItems.save(item);
    }

    private LocalDateTime activityCreatedAt(ActivityVisibilityChangedEvent event) {
        if (event.activityCreatedAt() != null) {
            return toLocal(event.activityCreatedAt());
        }
        // Degrade to the snapshot's creation marker, then to "now" — only the
        // feed sort position is affected, never correctness.
        return feedItems.findByActivityId(event.activityId())
                .map(FeedItem::getOccurredAt)
                .orElseGet(LocalDateTime::now);
    }

    private static LocalDateTime toLocal(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    /** Best-effort "John Doe" -> [John, Doe]; single-word names become firstName only. */
    static String[] splitDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return new String[]{null, null};
        }
        String trimmed = displayName.trim();
        int space = trimmed.indexOf(' ');
        if (space < 0) {
            return new String[]{trimmed, null};
        }
        return new String[]{trimmed.substring(0, space), trimmed.substring(space + 1).trim()};
    }
}
