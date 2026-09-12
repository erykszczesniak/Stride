package com.strava.aiservice.guard

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@DisplayName("AiRateLimiter")
class AiRateLimiterTest {

    private class MutableClock(var now: Instant) : Clock() {
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId?) = this
        override fun instant(): Instant = now
    }

    private val clock = MutableClock(Instant.parse("2026-09-12T10:00:00Z"))
    private val limiter = AiRateLimiter(clock, windowMillis = 60_000)

    @Test
    @DisplayName("allows the limit, rejects the next hit, and does not count rejected hits")
    fun allowsUpToLimit() {
        repeat(3) { assertThat(limiter.tryAcquire("chat", 1, 3)).isTrue() }
        assertThat(limiter.tryAcquire("chat", 1, 3)).isFalse()
        assertThat(limiter.tryAcquire("chat", 1, 3)).isFalse()
        // Rejected hits did not extend the window: after 60 s the user is clean again.
        clock.now = clock.now.plusSeconds(60)
        assertThat(limiter.tryAcquire("chat", 1, 3)).isTrue()
    }

    @Test
    @DisplayName("window slides: old hits expire individually")
    fun windowSlides() {
        assertThat(limiter.tryAcquire("chat", 1, 2)).isTrue()
        clock.now = clock.now.plusSeconds(30)
        assertThat(limiter.tryAcquire("chat", 1, 2)).isTrue()
        assertThat(limiter.tryAcquire("chat", 1, 2)).isFalse()
        clock.now = clock.now.plusSeconds(31) // first hit is now 61 s old
        assertThat(limiter.tryAcquire("chat", 1, 2)).isTrue()
    }

    @Test
    @DisplayName("users and buckets are independent")
    fun independentBuckets() {
        assertThat(limiter.tryAcquire("chat", 1, 1)).isTrue()
        assertThat(limiter.tryAcquire("chat", 1, 1)).isFalse()
        assertThat(limiter.tryAcquire("chat", 2, 1)).isTrue()
        assertThat(limiter.tryAcquire("generation", 1, 1)).isTrue()
    }

    @Test
    @DisplayName("evictIdle drops windows that fell outside the period")
    fun evictsIdle() {
        limiter.tryAcquire("chat", 1, 5)
        limiter.tryAcquire("chat", 2, 5)
        assertThat(limiter.trackedUsers()).isEqualTo(2)
        clock.now = clock.now.plusSeconds(61)
        limiter.evictIdle()
        assertThat(limiter.trackedUsers()).isZero()
    }
}
