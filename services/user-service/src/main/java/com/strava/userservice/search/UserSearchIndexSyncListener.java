package com.strava.userservice.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Keeps the Elasticsearch {@code users} index in sync with the canonical {@code users}
 * table. The listener is {@link Async} so that the HTTP request that triggered the user
 * save does not wait for the ES round-trip, and failures are logged but swallowed — ES
 * is an eventually-consistent projection and must never block a DB write.
 */
@Slf4j
@Component
@Profile("!standalone")
@RequiredArgsConstructor
public class UserSearchIndexSyncListener {

    private final UserSearchRepository userSearchRepository;

    @Async
    @EventListener
    public void onUserUpserted(UserUpsertedEvent event) {
        try {
            UserDocument doc = UserDocument.fromUser(event.user());
            userSearchRepository.save(doc);
            log.debug("Indexed user {} into Elasticsearch", doc.getId());
        } catch (Exception e) {
            log.warn("Failed to index user {} into Elasticsearch: {}",
                    event.user() != null ? event.user().getId() : null, e.getMessage());
        }
    }

    @Async
    @EventListener
    public void onUserDeleted(UserDeletedEvent event) {
        try {
            userSearchRepository.deleteById(event.userId());
            log.debug("Removed user {} from Elasticsearch", event.userId());
        } catch (Exception e) {
            log.warn("Failed to remove user {} from Elasticsearch: {}",
                    event.userId(), e.getMessage());
        }
    }
}
