package com.strava.socialservice.config;

import com.strava.socialservice.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Standalone-profile seeder. Without Kafka the read-side consumers never fire,
 * so the feed would be empty — this populates a couple of FeedItems and the
 * matching UserCache rows directly so the GET /api/feed endpoint has data.
 */
@Slf4j
@Component
@Profile("standalone")
@RequiredArgsConstructor
public class StandaloneSeeder implements CommandLineRunner {

    private final FeedItemRepository feed;
    private final UserCacheRepository users;

    @Override
    public void run(String... args) {
        log.info("[standalone] Seeding user_cache + feed_items");
        LocalDateTime now = LocalDateTime.now();

        users.save(UserCache.builder().userId(1L).displayName("Test User").updatedAt(now).build());
        users.save(UserCache.builder().userId(2L).displayName("Alice Smith").updatedAt(now).build());

        feed.save(FeedItem.builder()
                .activityId(101L).userId(1L).sport("RUN")
                .distanceMeters(10_000.0).durationSeconds(3000L).elevationGainMeters(50.0)
                .startedAt(now.minusHours(2)).occurredAt(now).build());
        feed.save(FeedItem.builder()
                .activityId(102L).userId(2L).sport("RIDE")
                .distanceMeters(40_000.0).durationSeconds(5400L).elevationGainMeters(200.0)
                .startedAt(now.minusHours(1)).occurredAt(now).build());
        feed.save(FeedItem.builder()
                .activityId(103L).userId(1L).sport("SWIM")
                .distanceMeters(1500.0).durationSeconds(1800L).elevationGainMeters(0.0)
                .startedAt(now.minusMinutes(30)).occurredAt(now).build());
    }
}
