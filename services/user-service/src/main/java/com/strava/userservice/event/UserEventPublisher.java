package com.strava.userservice.event;

import com.strava.shared.events.Topics;
import com.strava.shared.events.UserDeletedEvent;
import com.strava.shared.events.UserFollowedEvent;
import com.strava.shared.events.UserRegisteredEvent;
import com.strava.shared.events.UserUnfollowedEvent;
import com.strava.shared.events.UserUpdatedEvent;
import com.strava.userservice.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Publishes user domain events to Kafka. All sends are best-effort — a transient broker
 * outage must never block a user-facing write. If the broker is genuinely down for an
 * extended period this loses events; we accept that for now (an outbox table would be
 * the production-grade fix).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventPublisher {

    private final KafkaTemplate<String, Object> kafka;

    public void publishUserRegistered(User user) {
        send(Topics.USER_REGISTERED, user.getId(), new UserRegisteredEvent(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatar(),
                Instant.now()));
    }

    public void publishUserUpdated(User user) {
        send(Topics.USER_UPDATED, user.getId(), new UserUpdatedEvent(
                user.getId(),
                user.getFullName(),
                user.getAvatar(),
                Instant.now()));
    }

    public void publishUserDeleted(Long userId) {
        send(Topics.USER_DELETED, userId, new UserDeletedEvent(userId, Instant.now()));
    }

    /** {@code followerDisplayName} rides along so notification-service can render the FOLLOW notification. */
    public void publishUserFollowed(User follower, Long followeeUserId) {
        send(Topics.USER_FOLLOWED, followeeUserId, new UserFollowedEvent(
                follower.getId(),
                followeeUserId,
                follower.getFullName(),
                Instant.now()));
    }

    public void publishUserUnfollowed(Long followerUserId, Long followeeUserId) {
        send(Topics.USER_UNFOLLOWED, followeeUserId,
                new UserUnfollowedEvent(followerUserId, followeeUserId, Instant.now()));
    }

    /** Keyed by the affected user id so per-user event ordering is preserved per partition. */
    private void send(String topic, Long key, Object event) {
        try {
            kafka.send(topic, String.valueOf(key), event)
                    .whenComplete((res, ex) -> {
                        if (ex != null) {
                            log.warn("Failed to publish {} to {}: {}",
                                    event.getClass().getSimpleName(), topic, ex.getMessage());
                        } else {
                            log.debug("Published {} topic={} partition={}",
                                    event.getClass().getSimpleName(), topic,
                                    res.getRecordMetadata().partition());
                        }
                    });
        } catch (Exception e) {
            log.warn("Synchronous failure publishing {} to {}: {}",
                    event.getClass().getSimpleName(), topic, e.getMessage());
        }
    }
}
