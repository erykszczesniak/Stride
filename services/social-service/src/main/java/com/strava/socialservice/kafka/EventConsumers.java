package com.strava.socialservice.kafka;

import com.strava.shared.events.*;
import com.strava.socialservice.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Read-side projections. Each consumer updates the local read model based on
 * domain events from other services. Consumers are idempotent where possible
 * — UserCache uses upsert, FeedItem checks activity-id uniqueness before
 * insert. Counter updates (comment/reaction) are at-least-once and may over-
 * count in pathological replay scenarios; the saga/observability phase adds
 * proper idempotency keys + DLQ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventConsumers {

    private final FeedItemRepository feed;
    private final UserCacheRepository users;

    @KafkaListener(topics = Topics.USER_REGISTERED, groupId = "social-service")
    @Transactional
    public void onUserRegistered(UserRegisteredEvent event) {
        log.debug("Caching user userId={} displayName={}", event.userId(), event.displayName());
        users.findById(event.userId())
                .ifPresentOrElse(
                        existing -> {
                            existing.setDisplayName(event.displayName());
                            existing.setAvatar(event.avatar());
                            existing.setUpdatedAt(LocalDateTime.now());
                        },
                        () -> users.save(UserCache.builder()
                                .userId(event.userId())
                                .displayName(event.displayName())
                                .avatar(event.avatar())
                                .updatedAt(LocalDateTime.now())
                                .build()));
    }

    @KafkaListener(topics = Topics.ACTIVITY_CREATED, groupId = "social-service")
    @Transactional
    public void onActivityCreated(ActivityCreatedEvent event) {
        log.debug("Projecting activity into feed activityId={} userId={}", event.activityId(), event.userId());
        if (feed.findByActivityId(event.activityId()).isPresent()) return; // idempotent replay
        feed.save(FeedItem.builder()
                .activityId(event.activityId())
                .userId(event.userId())
                .sport(event.sport())
                .distanceMeters(event.distanceMeters())
                .durationSeconds(event.durationSeconds())
                .elevationGainMeters(event.elevationGainMeters())
                .startedAt(event.startedAt() != null
                        ? LocalDateTime.ofInstant(event.startedAt(), ZoneOffset.UTC)
                        : LocalDateTime.now())
                .occurredAt(LocalDateTime.ofInstant(event.occurredAt(), ZoneOffset.UTC))
                .build());
    }

    @KafkaListener(topics = Topics.COMMENT_ADDED, groupId = "social-service-counts")
    @Transactional
    public void onCommentAdded(CommentAddedEvent event) {
        int updated = feed.incrementCommentCount(event.activityId());
        if (updated == 0) {
            log.debug("No feed item to increment for commented activityId={} (event arrived before ActivityCreated)",
                    event.activityId());
        }
    }

    @KafkaListener(topics = Topics.REACTION_ADDED, groupId = "social-service-counts")
    @Transactional
    public void onReactionAdded(ReactionAddedEvent event) {
        int updated = feed.incrementReactionCount(event.activityId());
        if (updated == 0) {
            log.debug("No feed item to increment for reacted activityId={} (event arrived before ActivityCreated)",
                    event.activityId());
        }
    }
}
