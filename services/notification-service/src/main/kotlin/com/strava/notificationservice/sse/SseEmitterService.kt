package com.strava.notificationservice.sse

import com.strava.notificationservice.notification.NotificationDto
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Port of the monolith's SseEmitterServiceImpl: one emitter per user
 * (a reconnect replaces the previous one), 5-minute timeout, an initial
 * `connected` event, `notification` events carrying NotificationDTO JSON,
 * and 25s heartbeat comments that evict dead emitters.
 *
 * In-memory and single-pod by design — a multi-replica deployment would
 * need a Redis pub/sub fanout (deferred to the scale phase).
 */
@Service
class SseEmitterService {

    private val log = LoggerFactory.getLogger(javaClass)
    private val emitters = ConcurrentHashMap<Long, SseEmitter>()

    fun createEmitter(userId: Long): SseEmitter {
        removeEmitter(userId)

        val emitter = SseEmitter(SSE_TIMEOUT)

        emitter.onCompletion {
            log.debug("SSE connection completed for user {}", userId)
            emitters.remove(userId)
        }
        emitter.onTimeout {
            log.debug("SSE connection timed out for user {}", userId)
            emitters.remove(userId)
        }
        emitter.onError { e ->
            log.debug("SSE connection error for user {}: {}", userId, e.message)
            emitters.remove(userId)
        }

        emitters[userId] = emitter
        log.debug("SSE emitter created for user {}", userId)

        try {
            emitter.send(
                SseEmitter.event()
                    .name("connected")
                    .data("Connection established")
            )
        } catch (e: IOException) {
            log.debug("Failed to send initial SSE event to user {}", userId)
            emitters.remove(userId)
        }

        return emitter
    }

    fun sendNotification(userId: Long, notification: NotificationDto) {
        val emitter = emitters[userId] ?: return
        try {
            emitter.send(
                SseEmitter.event()
                    .name("notification")
                    .data(notification)
            )
            log.debug("SSE notification sent to user {}", userId)
        } catch (e: IOException) {
            log.debug("Failed to send SSE notification to user {}, removing emitter", userId)
            emitters.remove(userId)
        }
    }

    @Scheduled(fixedRate = 25000)
    fun sendHeartbeats() {
        emitters.forEach { (userId, emitter) ->
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"))
            } catch (e: IOException) {
                log.debug("Heartbeat failed for user {}, removing emitter", userId)
                emitters.remove(userId)
            }
        }
    }

    fun removeEmitter(userId: Long) {
        val emitter = emitters.remove(userId) ?: return
        try {
            emitter.complete()
        } catch (e: Exception) {
            // Emitter already completed or errored
        }
    }

    fun hasActiveEmitter(userId: Long): Boolean = emitters.containsKey(userId)

    companion object {
        private const val SSE_TIMEOUT = 300_000L // 5 minutes
    }
}
