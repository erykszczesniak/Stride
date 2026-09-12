package com.strava.aiservice.guard

import java.time.Clock
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap

/**
 * Per-user sliding-window limiter for provider-billed calls. Kept in memory
 * on purpose: a chat turn costs real money and the limit is per replica —
 * with N replicas a user gets at most N× the limit, which is acceptable for
 * an abuse brake (the gateway's Redis rate limiter is the fairness layer).
 *
 * No Resilience4j RateLimiter here: one registry entry per user would also
 * mean one set of Micrometer series per user, i.e. unbounded cardinality.
 * Windows are pruned on every check, and idle users are evicted lazily.
 */
class AiRateLimiter(
    private val clock: Clock = Clock.systemUTC(),
    private val windowMillis: Long = 60_000,
) {

    private class Window {
        val hits = ArrayDeque<Long>()
    }

    private val windows = ConcurrentHashMap<String, Window>()

    /**
     * Records one hit for `(bucket, userId)` and returns true when it is
     * within `limitPerWindow`. Rejected hits are not recorded, so a user
     * who keeps hammering does not push their own recovery further away.
     */
    fun tryAcquire(bucket: String, userId: Long, limitPerWindow: Int): Boolean {
        val now = clock.millis()
        val key = "$bucket:$userId"
        val window = windows.computeIfAbsent(key) { Window() }
        synchronized(window) {
            while (window.hits.isNotEmpty() && now - window.hits.peekFirst() >= windowMillis) {
                window.hits.pollFirst()
            }
            if (window.hits.size >= limitPerWindow) return false
            window.hits.addLast(now)
            return true
        }
    }

    /** Drops windows with no hits inside the current window; call from a scheduler if needed. */
    fun evictIdle() {
        val now = clock.millis()
        windows.entries.removeIf { (_, w) ->
            synchronized(w) { w.hits.isEmpty() || now - w.hits.peekLast() >= windowMillis }
        }
    }

    internal fun trackedUsers(): Int = windows.size
}
