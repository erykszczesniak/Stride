package com.strava.backend.ai.telemetry;

import java.util.function.Supplier;

/**
 * Who is asking, carried from the service layer to the provider clients
 * without changing the client interfaces. Thread-bound like MDC; the clients
 * read it when they record the call, the services set it around the
 * provider call.
 */
public final class AiCallContext {

    public record Ctx(Long userId, Long activityId) { }

    private static final ThreadLocal<Ctx> HOLDER = new ThreadLocal<>();

    private AiCallContext() {
    }

    /** The current caller, or null outside {@link #with}. */
    public static Ctx current() {
        return HOLDER.get();
    }

    public static <T> T with(Long userId, Long activityId, Supplier<T> block) {
        Ctx previous = HOLDER.get();
        HOLDER.set(new Ctx(userId, activityId));
        try {
            return block.get();
        } finally {
            if (previous == null) HOLDER.remove(); else HOLDER.set(previous);
        }
    }
}
