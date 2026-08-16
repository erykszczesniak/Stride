package com.strava.socialservice.projection;

import com.strava.socialservice.domain.FeedItem;
import com.strava.socialservice.domain.FeedItemRepository;
import com.strava.socialservice.domain.FollowEdgeRepository;
import com.strava.socialservice.domain.UserFeedEntry;
import com.strava.socialservice.domain.UserFeedEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Keeps the {@code user_feed_entries} read model in sync — semantics ported
 * 1:1 from main's UserFeedProjector, with two substitutions dictated by the
 * service split:
 *
 *   * follower ids come from the local {@code follow_edges} projection
 *     instead of UserRepository;
 *   * the follow-backfill source is the local {@code feed_items} snapshot
 *     instead of ActivityRepository.
 *
 * Main drove this off AFTER_COMMIT application events; here the same handlers
 * are invoked by the Kafka consumers, which pushes the eventual-consistency
 * window from ~0 to broker latency — acceptable for a feed, and the explicit
 * design goal of this phase.
 *
 * <p>Idempotency: every handler subtracts existing rows before inserting new
 * ones, so redelivered events are safe. The unique constraint on
 * {@code (recipient_user_id, activity_id)} is the last-line defense.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserFeedProjector {

    /** Backfill horizon when a user follows someone — how many of the followee's recent public activities to materialize. */
    static final int FOLLOW_BACKFILL_LIMIT = 50;

    private final UserFeedEntryRepository feedRepository;
    private final FollowEdgeRepository followEdgeRepository;
    private final FeedItemRepository feedItemRepository;

    public void onActivityCreated(Long activityId, Long actorUserId, boolean isPrivate,
                                  LocalDateTime activityCreatedAt) {
        // Author always sees their own activity (public or private) in their own feed.
        Set<Long> recipientIds = new HashSet<>();
        recipientIds.add(actorUserId);

        // Followers only see PUBLIC activities — private activities never fan out.
        if (!isPrivate) {
            recipientIds.addAll(followEdgeRepository.findFollowerIdsByFolloweeId(actorUserId));
        }

        insertFeedRows(recipientIds, activityId, actorUserId, activityCreatedAt);
        log.debug("Fanned out activity {} to {} feed(s)", activityId, recipientIds.size());
    }

    public void onActivityDeleted(Long activityId) {
        int removed = feedRepository.deleteByActivityId(activityId);
        log.debug("Removed {} feed row(s) for deleted activity {}", removed, activityId);
    }

    public void onActivityVisibilityChanged(Long activityId, Long actorUserId, boolean nowPrivate,
                                            LocalDateTime activityCreatedAt) {
        if (nowPrivate) {
            // public → private: keep only the author's own row.
            int removed = feedRepository.deleteByActivityIdExceptRecipient(activityId, actorUserId);
            log.debug("Retracted activity {} from {} follower feed(s) after privacy flip",
                    activityId, removed);
        } else {
            // private → public: re-fan-out to current followers.
            Set<Long> recipientIds = new HashSet<>(
                    followEdgeRepository.findFollowerIdsByFolloweeId(actorUserId));
            insertFeedRows(recipientIds, activityId, actorUserId, activityCreatedAt);
            log.debug("Re-fanned activity {} to {} follower feed(s) after privacy flip",
                    activityId, recipientIds.size());
        }
    }

    public void onUserFollowed(Long followerUserId, Long followeeUserId) {
        // Backfill the new follower's feed with the followee's recent public
        // activities. Bounded by FOLLOW_BACKFILL_LIMIT so following a power
        // user doesn't dump thousands of rows in one transaction.
        List<FeedItem> recent = feedItemRepository.findRecentPublicByUserId(
                followeeUserId, PageRequest.of(0, FOLLOW_BACKFILL_LIMIT));
        if (recent.isEmpty()) {
            return;
        }

        Set<Long> alreadyMaterialized = new HashSet<>(
                feedRepository.findActivityIdsByRecipientUserIdAndActorUserId(
                        followerUserId, followeeUserId));

        List<UserFeedEntry> newRows = recent.stream()
                .filter(f -> !alreadyMaterialized.contains(f.getActivityId()))
                .map(f -> UserFeedEntry.builder()
                        .recipientUserId(followerUserId)
                        .activityId(f.getActivityId())
                        .actorUserId(followeeUserId)
                        .activityCreatedAt(f.getOccurredAt())
                        .build())
                .toList();

        if (!newRows.isEmpty()) {
            feedRepository.saveAll(newRows);
            log.debug("Backfilled {} activities into user {}'s feed from user {}",
                    newRows.size(), followerUserId, followeeUserId);
        }
    }

    public void onUserUnfollowed(Long followerUserId, Long followeeUserId) {
        int removed = feedRepository.deleteByRecipientUserIdAndActorUserId(
                followerUserId, followeeUserId);
        log.debug("Purged {} feed row(s) after user {} unfollowed user {}",
                removed, followerUserId, followeeUserId);
    }

    /**
     * Subtract recipients that already have a row for this activity (idempotency),
     * then bulk-insert the rest. Avoids relying on the unique constraint to
     * reject duplicates, which would abort the whole transaction.
     */
    private void insertFeedRows(Set<Long> recipientIds, Long activityId, Long actorUserId,
                                LocalDateTime activityCreatedAt) {
        if (recipientIds.isEmpty()) {
            return;
        }
        Set<Long> existing = new HashSet<>(feedRepository.findRecipientIdsByActivityId(activityId));
        recipientIds.removeAll(existing);
        if (recipientIds.isEmpty()) {
            return;
        }

        List<UserFeedEntry> rows = recipientIds.stream()
                .map(rid -> UserFeedEntry.builder()
                        .recipientUserId(rid)
                        .activityId(activityId)
                        .actorUserId(actorUserId)
                        .activityCreatedAt(activityCreatedAt)
                        .build())
                .toList();
        feedRepository.saveAll(rows);
    }
}
