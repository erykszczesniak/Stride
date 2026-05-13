package com.strava.challengegoal.kafka;

import com.strava.shared.events.ActivityCreatedEvent;
import com.strava.shared.events.Topics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * When a new activity lands we don't know which (user, sport, window) keys
 * it would invalidate — the cache key embeds the date window of the goal/
 * challenge, not the activity. Cheapest correct option: wipe the entire
 * activity-summary cache for that user.
 *
 * Spring's ConcurrentMapCache / RedisCacheManager doesn't support pattern
 * eviction out of the box; we just call clear() on the whole cache. For
 * the load profile we expect (low write rate vs. high read rate from the
 * dashboard) the impact is fine. If write rate grows we'd switch to a
 * RedisTemplate-based pattern-scan eviction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityEventConsumer {

    private final CacheManager cacheManager;

    @KafkaListener(topics = Topics.ACTIVITY_CREATED, groupId = "challenge-goal-service")
    public void onActivityCreated(ActivityCreatedEvent event) {
        log.debug("Invalidating activity-summary cache after ActivityCreated userId={} activityId={}",
                event.userId(), event.activityId());
        var cache = cacheManager.getCache("activity-summary");
        if (cache != null) cache.clear();
    }
}
