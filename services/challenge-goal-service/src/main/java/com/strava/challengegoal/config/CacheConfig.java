package com.strava.challengegoal.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
public class CacheConfig {

    /**
     * Production cache: Redis with a 30s TTL on activity-summary entries.
     * 30 seconds is short enough that stale data isn't a problem for progress
     * bars and long enough to absorb dashboard refresh storms. Active
     * invalidation by ActivityCreated event makes the TTL a safety net rather
     * than the primary freshness mechanism.
     */
    @Bean
    @Profile("!standalone")
    public CacheManager redisCacheManager(RedisConnectionFactory cf) {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.strava.challengegoal")
                .allowIfSubType("java.util")
                .allowIfSubType("java.lang")
                .allowIfSubType("java.time")
                .build();
        mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(30))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(mapper)));
        return RedisCacheManager.builder(cf)
                .cacheDefaults(config)
                .withCacheConfiguration("activity-summary", config)
                .build();
    }

    /**
     * Standalone profile: in-process cache. Same @Cacheable code path, no
     * Redis required for local smoke tests.
     */
    @Bean
    @Profile("standalone")
    public CacheManager localCacheManager() {
        return new ConcurrentMapCacheManager("activity-summary");
    }
}
