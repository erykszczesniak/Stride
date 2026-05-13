package com.strava.notificationservice.notification

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val service: NotificationService
) {

    @GetMapping
    fun list(@AuthenticationPrincipal userId: Long, pageable: Pageable): Page<NotificationDto> =
        service.list(userId, pageable)

    @GetMapping("/unread/count")
    fun unreadCount(@AuthenticationPrincipal userId: Long): Map<String, Long> =
        mapOf("count" to service.unreadCount(userId))

    @PostMapping("/{id}/read")
    fun markRead(@AuthenticationPrincipal userId: Long, @PathVariable id: Long): NotificationDto =
        service.markRead(id, userId)

    @PostMapping("/read-all")
    fun markAllRead(@AuthenticationPrincipal userId: Long): Map<String, Int> =
        mapOf("updated" to service.markAllRead(userId))
}
