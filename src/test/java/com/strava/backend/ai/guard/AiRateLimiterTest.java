package com.strava.backend.ai.guard;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AiRateLimiter")
class AiRateLimiterTest {

    private static final class MutableClock extends Clock {
        Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final MutableClock clock = new MutableClock(Instant.parse("2026-09-12T10:00:00Z"));
    private final AiRateLimiter limiter = new AiRateLimiter(clock, 60_000);

    @Test
    @DisplayName("allows the limit, rejects the next hit, and does not count rejected hits")
    void allowsUpToLimit() {
        for (int i = 0; i < 3; i++) assertThat(limiter.tryAcquire("chat", 1, 3)).isTrue();
        assertThat(limiter.tryAcquire("chat", 1, 3)).isFalse();
        assertThat(limiter.tryAcquire("chat", 1, 3)).isFalse();
        // Rejected hits did not extend the window: after 60 s the user is clean again.
        clock.now = clock.now.plusSeconds(60);
        assertThat(limiter.tryAcquire("chat", 1, 3)).isTrue();
    }

    @Test
    @DisplayName("window slides: old hits expire individually")
    void windowSlides() {
        assertThat(limiter.tryAcquire("chat", 1, 2)).isTrue();
        clock.now = clock.now.plusSeconds(30);
        assertThat(limiter.tryAcquire("chat", 1, 2)).isTrue();
        assertThat(limiter.tryAcquire("chat", 1, 2)).isFalse();
        clock.now = clock.now.plusSeconds(31); // first hit is now 61 s old
        assertThat(limiter.tryAcquire("chat", 1, 2)).isTrue();
    }

    @Test
    @DisplayName("users and buckets are independent")
    void independentBuckets() {
        assertThat(limiter.tryAcquire("chat", 1, 1)).isTrue();
        assertThat(limiter.tryAcquire("chat", 1, 1)).isFalse();
        assertThat(limiter.tryAcquire("chat", 2, 1)).isTrue();
        assertThat(limiter.tryAcquire("generation", 1, 1)).isTrue();
    }

    @Test
    @DisplayName("evictIdle drops windows that fell outside the period")
    void evictsIdle() {
        limiter.tryAcquire("chat", 1, 5);
        limiter.tryAcquire("chat", 2, 5);
        assertThat(limiter.trackedUsers()).isEqualTo(2);
        clock.now = clock.now.plusSeconds(61);
        limiter.evictIdle();
        assertThat(limiter.trackedUsers()).isZero();
    }
}
