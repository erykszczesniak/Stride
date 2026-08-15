package com.strava.socialservice.event;

import com.strava.shared.events.CommentAddedEvent;
import com.strava.shared.events.ReactionAddedEvent;
import com.strava.shared.events.Topics;
import com.strava.socialservice.domain.Comment;
import com.strava.socialservice.domain.Reaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

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

    public void publishReactionAdded(Reaction r) {
        ReactionAddedEvent event = new ReactionAddedEvent(
                r.getActivityId(), r.getUserId(), r.getReactionType(), Instant.now()
        );
        send(Topics.REACTION_ADDED, String.valueOf(r.getActivityId()), event);
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
