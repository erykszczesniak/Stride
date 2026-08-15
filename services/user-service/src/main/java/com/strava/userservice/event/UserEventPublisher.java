package com.strava.userservice.event;

import com.strava.shared.events.Topics;
import com.strava.shared.events.UserRegisteredEvent;
import com.strava.userservice.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventPublisher {

    private final KafkaTemplate<String, Object> kafka;

    public void publishUserRegistered(User user) {
        UserRegisteredEvent event = new UserRegisteredEvent(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatar(),
                Instant.now()
        );
        // Best-effort publish — never let a transient broker outage block registration.
        // If the broker is genuinely down for an extended period this loses events; we
        // accept that for now (an outbox table would be the production-grade fix).
        try {
            kafka.send(Topics.USER_REGISTERED, String.valueOf(user.getId()), event)
                    .whenComplete((res, ex) -> {
                        if (ex != null) {
                            log.warn("Failed to publish UserRegisteredEvent for userId={}: {}",
                                    user.getId(), ex.getMessage());
                        } else {
                            log.debug("Published UserRegisteredEvent userId={} topic={} partition={}",
                                    user.getId(), Topics.USER_REGISTERED,
                                    res.getRecordMetadata().partition());
                        }
                    });
        } catch (Exception e) {
            log.warn("Synchronous failure publishing UserRegisteredEvent for userId={}: {}",
                    user.getId(), e.getMessage());
        }
    }
}
