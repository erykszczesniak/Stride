package com.strava.backend.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Hand-rolled sliding-window rate limiter for the Strava API.
 * <p>
 * Strava's published limits (non-enterprise): 100 requests per 15 minutes and
 * 1,000 requests per day. We track the timestamp of each call and sleep if the window
 * is full. This is process-local — good enough for a single-instance portfolio deploy;
 * a real multi-instance rollout would move the counters to Redis.
 * <p>
 * {@link #notify429()} also backs off if Strava returns HTTP 429 before we hit our
 * internal ceilings.
 */
@Slf4j
@Component
public class StravaRateLimiter {

    static final int SHORT_WINDOW_LIMIT = 100;
    static final Duration SHORT_WINDOW = Duration.ofMinutes(15);
    static final int DAILY_LIMIT = 1_000;
    static final Duration DAILY_WINDOW = Duration.ofDays(1);

    private final Deque<Instant> shortWindow = new ArrayDeque<>();
    private final Deque<Instant> dailyWindow = new ArrayDeque<>();
    private volatile Instant backoffUntil = Instant.EPOCH;

    /**
     * Block until a call may proceed, then record it. Callers should invoke this
     * immediately before dispatching the HTTP request.
     */
    public synchronized void acquire() throws InterruptedException {
        long now = System.currentTimeMillis();
        // Honour an explicit back-off first (set by notify429).
        Instant boUntil = backoffUntil;
        if (Instant.ofEpochMilli(now).isBefore(boUntil)) {
            long waitMs = boUntil.toEpochMilli() - now;
            log.warn("Strava back-off active, sleeping {}ms", waitMs);
            Thread.sleep(waitMs);
        }

        evictOld(shortWindow, SHORT_WINDOW);
        evictOld(dailyWindow, DAILY_WINDOW);

        while (shortWindow.size() >= SHORT_WINDOW_LIMIT) {
            long waitMs = Math.max(
                    1,
                    shortWindow.peekFirst().plus(SHORT_WINDOW).toEpochMilli() - System.currentTimeMillis());
            log.warn("Strava short-window ({}/15min) full; sleeping {}ms", SHORT_WINDOW_LIMIT, waitMs);
            Thread.sleep(waitMs);
            evictOld(shortWindow, SHORT_WINDOW);
        }

        while (dailyWindow.size() >= DAILY_LIMIT) {
            long waitMs = Math.max(
                    1,
                    dailyWindow.peekFirst().plus(DAILY_WINDOW).toEpochMilli() - System.currentTimeMillis());
            log.warn("Strava daily limit ({}/day) reached; sleeping {}ms", DAILY_LIMIT, waitMs);
            Thread.sleep(waitMs);
            evictOld(dailyWindow, DAILY_WINDOW);
        }

        Instant stamp = Instant.now();
        shortWindow.addLast(stamp);
        dailyWindow.addLast(stamp);
    }

    /** Called when the server returns 429 — back off for {@code retryAfter} seconds. */
    public void notify429(long retryAfterSeconds) {
        long seconds = Math.max(retryAfterSeconds, 60);
        this.backoffUntil = Instant.now().plusSeconds(seconds);
        log.warn("Received HTTP 429 from Strava; backing off until {}", backoffUntil);
    }

    private static void evictOld(Deque<Instant> window, Duration ttl) {
        Instant cutoff = Instant.now().minus(ttl);
        while (!window.isEmpty() && window.peekFirst().isBefore(cutoff)) {
            window.pollFirst();
        }
    }

    // Visible for testing.
    int shortWindowSize() {
        return shortWindow.size();
    }

    int dailyWindowSize() {
        return dailyWindow.size();
    }
}
