package com.strava.backend.search;

import com.strava.backend.event.UserDeletedEvent;
import com.strava.backend.event.UserUpsertedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Keeps the Elasticsearch {@code users} index in sync with the canonical
 * PostgreSQL {@code users} table. The listener is {@link Async} so that the
 * HTTP request that triggered the user save does not wait for the ES round-trip,
 * and failures are logged but swallowed — ES is an eventually-consistent
 * projection and must never block a DB write.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserSearchIndexSyncListener {

    private final UserSearchRepository userSearchRepository;

    @Async
    @EventListener
    public void onUserUpserted(UserUpsertedEvent event) {
        try {
            UserDocument doc = UserDocument.fromUser(event.getUser());
            userSearchRepository.save(doc);
            log.debug("Indexed user {} into Elasticsearch", doc.getId());
        } catch (Exception e) {
            log.warn("Failed to index user {} into Elasticsearch: {}",
                    event.getUser() != null ? event.getUser().getId() : null, e.getMessage());
        }
    }

    @Async
    @EventListener
    public void onUserDeleted(UserDeletedEvent event) {
        try {
            userSearchRepository.deleteById(event.getUserId());
            log.debug("Removed user {} from Elasticsearch", event.getUserId());
        } catch (Exception e) {
            log.warn("Failed to remove user {} from Elasticsearch: {}",
                    event.getUserId(), e.getMessage());
        }
    }
}
