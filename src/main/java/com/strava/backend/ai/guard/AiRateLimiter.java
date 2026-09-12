package com.strava.backend.ai.guard;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-user sliding-window limiter for provider-billed calls. Kept in memory
 * on purpose: a chat turn costs real money and the limit is per replica —
 * with N replicas a user gets at most N× the limit, which is acceptable for
 * an abuse brake.
 *
 * <p>No Micrometer series per user: one registry entry per user would mean
 * unbounded cardinality. Windows are pruned on every check, and idle users
 * are evicted lazily.
 */
public class AiRateLimiter {

    private static final class Window {
        final ArrayDeque<Long> hits = new ArrayDeque<>();
    }

    private final Clock clock;
    private final long windowMillis;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public AiRateLimiter() {
        this(Clock.systemUTC(), 60_000);
    }

    public AiRateLimiter(Clock clock, long windowMillis) {
        this.clock = clock;
        this.windowMillis = windowMillis;
    }

    /**
     * Records one hit for {@code (bucket, userId)} and returns true when it is
     * within {@code limitPerWindow}. Rejected hits are not recorded, so a user
     * who keeps hammering does not push their own recovery further away.
     */
    public boolean tryAcquire(String bucket, long userId, int limitPerWindow) {
        long now = clock.millis();
        Window window = windows.computeIfAbsent(bucket + ":" + userId, k -> new Window());
        synchronized (window) {
            while (!window.hits.isEmpty() && now - window.hits.peekFirst() >= windowMillis) {
                window.hits.pollFirst();
            }
            if (window.hits.size() >= limitPerWindow) return false;
            window.hits.addLast(now);
            return true;
        }
    }

    /** Drops windows with no hits inside the current window; call from a scheduler if needed. */
    public void evictIdle() {
        long now = clock.millis();
        windows.entrySet().removeIf(e -> {
            Window w = e.getValue();
            synchronized (w) {
                return w.hits.isEmpty() || now - w.hits.peekLast() >= windowMillis;
            }
        });
    }

    int trackedUsers() {
        return windows.size();
    }
}
