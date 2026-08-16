package com.strava.socialservice.event;

import com.strava.shared.events.ActivityLikedEvent;
import com.strava.shared.events.CommentAddedEvent;
import com.strava.shared.events.Topics;
import com.strava.socialservice.domain.Comment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Best-effort Kafka publishing with tight producer timeouts (configured via
 * spring.kafka.producer.properties) — a broker outage must never block a
 * like or a comment. Consumed by notification-service (LIKE / COMMENT
 * notifications). The old ReactionAddedEvent stream is gone: activity-level
 * likes replaced the KUDOS reaction model in Phase 16.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SocialEventPublisher {

    private final KafkaTemplate<String, Object> kafka;

    public void publishCommentAdded(Comment c, Long activityOwnerId, String authorDisplayName) {
        String preview = c.getContent().length() > 100
                ? c.getContent().substring(0, 100) + "…"
                : c.getContent();
        CommentAddedEvent event = new CommentAddedEvent(
                c.getId(), c.getActivityId(), activityOwnerId, c.getUserId(),
                authorDisplayName, preview, Instant.now()
        );
        send(Topics.COMMENT_ADDED, String.valueOf(c.getActivityId()), event);
    }

    public void publishActivityLiked(Long activityId, Long activityOwnerId,
                                     Long likerUserId, String likerDisplayName) {
        ActivityLikedEvent event = new ActivityLikedEvent(
                activityId, activityOwnerId, likerUserId, likerDisplayName, Instant.now()
        );
        send(Topics.ACTIVITY_LIKED, String.valueOf(activityId), event);
    }

    private void send(String topic, String key, Object payload) {
        try {
            kafka.send(topic, key, payload).whenComplete((res, ex) -> {
                if (ex != null) {
                    log.warn("Failed to publish to topic={}: {}", topic, ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.warn("Synchronous Kafka publish failure on topic={}: {}", topic, e.getMessage());
        }
    }
}
