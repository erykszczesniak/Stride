package com.strava.notificationservice.sse

import com.strava.notificationservice.notification.NotificationDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * In-memory SSE fanout keyed by recipient userId. Each browser tab is a
 * separate emitter in the list. When a notification is created, we iterate
 * the user's emitter list and push the event; failures evict the emitter.
 *
 * Single-pod design — for multi-replica we'd need a Redis pub/sub fanout
 * (each pod subscribes, sends only to its locally-attached emitters). That
 * upgrade is intentionally deferred to the observability/scale phase.
 */
@Service
class SseService {

    private val log = LoggerFactory.getLogger(javaClass)
    private val emitters = ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>>()
    private val timeoutMs = 30L * 60 * 1000 // 30 min — clients should reconnect

    fun subscribe(userId: Long): SseEmitter {
        val emitter = SseEmitter(timeoutMs)
        val list = emitters.computeIfAbsent(userId) { CopyOnWriteArrayList() }
        list.add(emitter)
        emitter.onCompletion { remove(userId, emitter) }
        emitter.onTimeout    { remove(userId, emitter) }
        emitter.onError      { _ -> remove(userId, emitter) }
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"))
        } catch (e: Exception) {
            log.debug("Initial SSE send failed for userId={}: {}", userId, e.message)
        }
        return emitter
    }

    fun push(userId: Long, payload: NotificationDto) {
        val list = emitters[userId] ?: return
        list.forEach { emitter ->
            try {
                emitter.send(SseEmitter.event().name("notification").data(payload))
            } catch (e: Exception) {
                remove(userId, emitter)
            }
        }
    }

    private fun remove(userId: Long, emitter: SseEmitter) {
        emitters[userId]?.remove(emitter)
        if (emitters[userId]?.isEmpty() == true) emitters.remove(userId)
    }
}
