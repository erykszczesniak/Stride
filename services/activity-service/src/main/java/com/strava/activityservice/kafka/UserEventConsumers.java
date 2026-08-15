package com.strava.activityservice.kafka;

import com.strava.activityservice.domain.UserCache;
import com.strava.activityservice.domain.UserCacheRepository;
import com.strava.shared.events.Topics;
import com.strava.shared.events.UserDeletedEvent;
import com.strava.shared.events.UserRegisteredEvent;
import com.strava.shared.events.UserUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Maintains the user_cache projection so activity responses can embed the
 * author without a per-row call to user-service. Upserts keyed by userId make
 * the listeners idempotent under at-least-once delivery.
 *
 * The events only carry a displayName ("John Doe"), while main's embedded
 * user object exposes firstName/lastName separately — the name is split
 * best-effort at the first space (single-word names become firstName only).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumers {

    private final UserCacheRepository users;

    @KafkaListener(topics = Topics.USER_REGISTERED, groupId = "activity-service")
    @Transactional
    public void onUserRegistered(UserRegisteredEvent event) {
        log.debug("Caching registered user userId={} displayName={}", event.userId(), event.displayName());
        upsert(event.userId(), event.displayName(), event.avatar());
    }

    @KafkaListener(topics = Topics.USER_UPDATED, groupId = "activity-service")
    @Transactional
    public void onUserUpdated(UserUpdatedEvent event) {
        log.debug("Refreshing cached user userId={} displayName={}", event.userId(), event.displayName());
        upsert(event.userId(), event.displayName(), event.avatar());
    }

    @KafkaListener(topics = Topics.USER_DELETED, groupId = "activity-service")
    @Transactional
    public void onUserDeleted(UserDeletedEvent event) {
        log.debug("Evicting cached user userId={}", event.userId());
        users.deleteById(event.userId());
    }

    private void upsert(Long userId, String displayName, String avatar) {
        String[] names = splitDisplayName(displayName);
        users.save(UserCache.builder()
                .userId(userId)
                .firstName(names[0])
                .lastName(names[1])
                .avatar(avatar)
                .updatedAt(LocalDateTime.now())
                .build());
    }

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
