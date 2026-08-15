package com.strava.notificationservice.sse

import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

/**
 * Mirrors the monolith's SseNotificationController:
 * GET /api/v1/notifications/stream (text/event-stream). The gateway
 * validates the EventSource `?token=` query param and injects X-User-Id
 * for this path, so by the time we run the principal is populated.
 */
@RestController
@RequestMapping("/api/v1/notifications")
class SseController(
    private val sseEmitterService: SseEmitterService
) {

    @GetMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamNotifications(@AuthenticationPrincipal userId: Long): SseEmitter =
        sseEmitterService.createEmitter(userId)
}
