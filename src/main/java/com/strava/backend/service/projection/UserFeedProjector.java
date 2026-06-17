package com.strava.backend.service.projection;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.UserFeedEntry;
import com.strava.backend.event.ActivityCreatedEvent;
import com.strava.backend.event.ActivityDeletedEvent;
import com.strava.backend.event.ActivityVisibilityChangedEvent;
import com.strava.backend.event.UserFollowedEvent;
import com.strava.backend.event.UserUnfollowedEvent;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.repository.UserFeedEntryRepository;
import com.strava.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Keeps the {@code user_feed_entries} read model in sync with the write side
 * by reacting to domain events. Runs AFTER the originating transaction commits
 * — so the projector never observes uncommitted writes and never has to undo
 * its own work after a rollback.
 *
 * <p>Idempotency: every handler subtracts existing rows before inserting new
 * ones, so re-firing an event is safe. The unique constraint on
 * {@code (recipient_user_id, activity_id)} is the last-line defense.</p>
 *
 * <p>Eventual consistency window: with {@code AFTER_COMMIT} the lag is
 * effectively zero (synchronous on the request thread). Switching to
 * {@code @Async} would push it to milliseconds and decouple write latency
 * from follower-count; moving to Kafka would push it to seconds. All three
 * are acceptable for a feed; this PR ships the simplest.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserFeedProjector {

    /** Backfill horizon when a user follows someone — how many of the followee's recent public activities to materialize. */
    private static final int FOLLOW_BACKFILL_LIMIT = 50;

    private final UserFeedEntryRepository feedRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onActivityCreated(ActivityCreatedEvent event) {
        // Author always sees their own activity (public or private) in their own feed.
        Set<Long> recipientIds = new HashSet<>();
        recipientIds.add(event.getActorUserId());

        // Followers only see PUBLIC activities — private activities never fan out.
        if (!event.isPrivate()) {
            recipientIds.addAll(userRepository.findFollowerIds(event.getActorUserId()));
        }

        insertFeedRows(recipientIds, event.getActivityId(), event.getActorUserId(),
                event.getActivityCreatedAt());
        log.debug("Fanned out activity {} to {} feed(s)", event.getActivityId(), recipientIds.size());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onActivityDeleted(ActivityDeletedEvent event) {
        int removed = feedRepository.deleteByActivityId(event.getActivityId());
        log.debug("Removed {} feed row(s) for deleted activity {}", removed, event.getActivityId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onActivityVisibilityChanged(ActivityVisibilityChangedEvent event) {
        if (event.isNowPrivate()) {
            // public → private: keep only the author's own row.
            int removed = feedRepository.deleteByActivityIdExceptRecipient(
                    event.getActivityId(), event.getActorUserId());
            log.debug("Retracted activity {} from {} follower feed(s) after privacy flip",
                    event.getActivityId(), removed);
        } else {
            // private → public: re-fan-out to current followers.
            Set<Long> recipientIds = new HashSet<>(
                    userRepository.findFollowerIds(event.getActorUserId()));
            insertFeedRows(recipientIds, event.getActivityId(), event.getActorUserId(),
                    event.getActivityCreatedAt());
            log.debug("Re-fanned activity {} to {} follower feed(s) after privacy flip",
                    event.getActivityId(), recipientIds.size());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onUserFollowed(UserFollowedEvent event) {
        // Backfill the new follower's feed with the followee's recent public activities.
        // Bounded by FOLLOW_BACKFILL_LIMIT so following a power user doesn't dump
        // thousands of rows in one transaction.
        List<Activity> recent = activityRepository.findRecentPublicByUserId(
                event.getFolloweeUserId(), PageRequest.of(0, FOLLOW_BACKFILL_LIMIT));
        if (recent.isEmpty()) {
            return;
        }

        Set<Long> alreadyMaterialized = new HashSet<>(
                feedRepository.findActivityIdsByRecipientUserIdAndActorUserId(
                        event.getFollowerUserId(), event.getFolloweeUserId()));

        List<UserFeedEntry> newRows = recent.stream()
                .filter(a -> !alreadyMaterialized.contains(a.getId()))
                .map(a -> UserFeedEntry.builder()
                        .recipientUserId(event.getFollowerUserId())
                        .activityId(a.getId())
                        .actorUserId(event.getFolloweeUserId())
                        .activityCreatedAt(a.getCreatedAt())
                        .build())
                .toList();

        if (!newRows.isEmpty()) {
            feedRepository.saveAll(newRows);
            log.debug("Backfilled {} activities into user {}'s feed from user {}",
                    newRows.size(), event.getFollowerUserId(), event.getFolloweeUserId());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onUserUnfollowed(UserUnfollowedEvent event) {
        int removed = feedRepository.deleteByRecipientUserIdAndActorUserId(
                event.getFollowerUserId(), event.getFolloweeUserId());
        log.debug("Purged {} feed row(s) after user {} unfollowed user {}",
                removed, event.getFollowerUserId(), event.getFolloweeUserId());
    }

    /**
     * Subtract recipients that already have a row for this activity (idempotency),
     * then bulk-insert the rest. Avoids relying on the unique constraint to
     * reject duplicates, which would abort the whole transaction.
     */
    private void insertFeedRows(Set<Long> recipientIds, Long activityId, Long actorUserId,
                                java.time.LocalDateTime activityCreatedAt) {
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
