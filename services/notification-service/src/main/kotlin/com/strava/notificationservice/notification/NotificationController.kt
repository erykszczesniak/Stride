package com.strava.notificationservice.notification

import com.strava.notificationservice.weekly.WeeklySummaryService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Public contract — byte-for-byte the monolith's NotificationController
 * under /api/v1/notifications. The weekly-summary trigger is ADMIN-only,
 * enforced in SecurityConfig via the gateway-injected X-User-Roles header.
 */
@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val notificationService: NotificationService,
    private val weeklySummaryService: WeeklySummaryService
) {

    @GetMapping
    fun getNotifications(@AuthenticationPrincipal userId: Long): ResponseEntity<List<NotificationDto>> =
        ResponseEntity.ok(notificationService.getNotifications(userId))

    @GetMapping("/unread-count")
    fun getUnreadCount(@AuthenticationPrincipal userId: Long): ResponseEntity<Map<String, Long>> =
        ResponseEntity.ok(mapOf("count" to notificationService.getUnreadCount(userId)))

    @PutMapping("/{notificationId}/read")
    fun markAsRead(
        @AuthenticationPrincipal userId: Long,
        @PathVariable notificationId: Long
    ): ResponseEntity<Map<String, String>> {
        notificationService.markAsRead(notificationId, userId)
        return ResponseEntity.ok(mapOf("message" to "Notification marked as read"))
    }

    @PutMapping("/read-all")
    fun markAllAsRead(@AuthenticationPrincipal userId: Long): ResponseEntity<Map<String, String>> {
        notificationService.markAllAsRead(userId)
        return ResponseEntity.ok(mapOf("message" to "All notifications marked as read"))
    }

    @DeleteMapping("/{notificationId}")
    fun deleteNotification(
        @AuthenticationPrincipal userId: Long,
        @PathVariable notificationId: Long
    ): ResponseEntity<Map<String, String>> {
        notificationService.deleteNotification(notificationId, userId)
        return ResponseEntity.ok(mapOf("message" to "Notification deleted"))
    }

    @DeleteMapping
    fun clearAllNotifications(@AuthenticationPrincipal userId: Long): ResponseEntity<Map<String, String>> {
        notificationService.clearAllNotifications(userId)
        return ResponseEntity.ok(mapOf("message" to "All notifications cleared"))
    }

    @PostMapping("/weekly-summary/trigger")
    fun triggerWeeklySummary(): ResponseEntity<Map<String, String>> {
        weeklySummaryService.sendWeeklySummaries()
        return ResponseEntity.ok(mapOf("message" to "Weekly summary triggered"))
    }
}
