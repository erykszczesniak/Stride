package com.strava.userservice.search;

import com.strava.userservice.domain.User;
import com.strava.userservice.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the Elasticsearch {@code users} index from the database on startup when the
 * index does not yet exist or is empty. Bounded to 1000 users to keep the cold-start
 * cost predictable on dev / fresh docker volumes. Any failure is logged and swallowed —
 * the service still boots if Elasticsearch is unavailable.
 */
@Slf4j
@Component
@Profile("!standalone")
@RequiredArgsConstructor
public class UserSearchIndexInitializer {

    private static final int MAX_SEED_USERS = 1000;

    private final UserRepository userRepository;
    private final UserSearchRepository userSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @EventListener(ApplicationReadyEvent.class)
    public void seedIndexIfEmpty() {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(UserDocument.class);
            if (!indexOps.exists()) {
                indexOps.create();
                indexOps.putMapping(indexOps.createMapping(UserDocument.class));
                log.info("Created Elasticsearch index 'users'");
            }

            long existing = userSearchRepository.count();
            if (existing > 0) {
                log.info("Elasticsearch 'users' index already populated ({} docs), skipping seed", existing);
                return;
            }

            List<User> users = userRepository.findAll(PageRequest.of(0, MAX_SEED_USERS)).getContent();
            if (users.isEmpty()) {
                log.info("No users in DB — nothing to seed into Elasticsearch");
                return;
            }

            List<UserDocument> docs = users.stream().map(UserDocument::fromUser).toList();
            userSearchRepository.saveAll(docs);
            log.info("Seeded {} users into Elasticsearch (capped at {})", docs.size(), MAX_SEED_USERS);
        } catch (Exception e) {
            log.warn("Elasticsearch seed on startup failed (search will still work once ES is reachable): {}",
                    e.getMessage());
        }
    }
}
