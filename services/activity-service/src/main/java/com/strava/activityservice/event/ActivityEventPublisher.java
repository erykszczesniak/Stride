package com.strava.activityservice.event;

import com.strava.activityservice.domain.Activity;
import com.strava.activityservice.domain.PersonalRecord;
import com.strava.shared.events.ActivityCreatedEvent;
import com.strava.shared.events.PersonalRecordAchievedEvent;
import com.strava.shared.events.Topics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Best-effort Kafka publishing. We use tight producer timeouts (configured via
 * spring.kafka.producer.properties) and swallow failures so a broker outage
 * cannot block activity creation. An outbox table is the production-grade fix
 * — TODO when we wire saga (Phase 9).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityEventPublisher {

    private final KafkaTemplate<String, Object> kafka;

    public void publishActivityCreated(Activity a) {
        ActivityCreatedEvent event = new ActivityCreatedEvent(
                a.getId(), a.getUserId(), a.getActivityType().name(),
                a.getTitle(), a.isPrivate(),
                a.getDistance() != null ? a.getDistance() * 1000.0 : null,
                a.getDuration() != null ? a.getDuration().longValue() : null,
                a.getElevationGain() != null ? a.getElevationGain().doubleValue() : null,
                a.getStartTime() != null ? a.getStartTime().toInstant(ZoneOffset.UTC) : null,
                Instant.now()
        );
        send(Topics.ACTIVITY_CREATED, String.valueOf(a.getId()), event);
    }

    public void publishActivityUpdated(Activity a) {
        // Reuse the same event DTO — Topics.ACTIVITY_UPDATED is a separate stream
        // so consumers can subscribe to "new activities" vs "any change" independently.
        ActivityCreatedEvent event = new ActivityCreatedEvent(
                a.getId(), a.getUserId(), a.getActivityType().name(),
                a.getTitle(), a.isPrivate(),
                a.getDistance() != null ? a.getDistance() * 1000.0 : null,
                a.getDuration() != null ? a.getDuration().longValue() : null,
                a.getElevationGain() != null ? a.getElevationGain().doubleValue() : null,
                a.getStartTime() != null ? a.getStartTime().toInstant(ZoneOffset.UTC) : null,
                Instant.now()
        );
        send(Topics.ACTIVITY_UPDATED, String.valueOf(a.getId()), event);
    }

    public void publishPersonalRecordAchieved(PersonalRecord pr) {
        PersonalRecordAchievedEvent event = new PersonalRecordAchievedEvent(
                pr.getUserId(), pr.getActivityId(),
                pr.getActivityType().name(), pr.getDistanceType().name(),
                pr.getTimeInSeconds().longValue(),
                Instant.now()
        );
        send(Topics.PERSONAL_RECORD_ACHIEVED, String.valueOf(pr.getUserId()), event);
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
