package com.strava.aiservice.telemetry

/**
 * Who is asking, carried from the service layer to the provider clients
 * without changing the client interfaces (the standalone mocks implement
 * them too). Thread-bound like MDC; the clients read it when they record
 * the call, the services set it around the provider call.
 */
object AiCallContext {

    data class Ctx(val userId: Long?, val activityId: Long?)

    private val holder = ThreadLocal<Ctx?>()

    fun current(): Ctx? = holder.get()

    fun <T> with(userId: Long?, activityId: Long? = null, block: () -> T): T {
        val previous = holder.get()
        holder.set(Ctx(userId, activityId))
        try {
            return block()
        } finally {
            if (previous == null) holder.remove() else holder.set(previous)
        }
    }
}
