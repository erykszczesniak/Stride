package com.strava.notificationservice.notification

import com.strava.notificationservice.config.SecurityConfig
import com.strava.notificationservice.domain.NotificationType
import com.strava.notificationservice.exception.ForbiddenException
import com.strava.notificationservice.exception.NotFoundException
import com.strava.notificationservice.sse.SseController
import com.strava.notificationservice.sse.SseEmitterService
import com.strava.notificationservice.weekly.WeeklySummaryService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.LocalDateTime

/**
 * Contract slice test: paths, auth (gateway-injected X-User-* headers),
 * response shapes and messages must match the monolith byte-for-byte.
 */
@WebMvcTest(
    controllers = [NotificationController::class, SseController::class],
    properties = [
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false"
    ]
)
@Import(SecurityConfig::class)
class NotificationControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var notificationService: NotificationService

    @MockitoBean
    lateinit var weeklySummaryService: WeeklySummaryService

    @MockitoBean
    lateinit var sseEmitterService: SseEmitterService

    private fun dto() = NotificationDto(
        id = 5, senderFirstName = "John", senderLastName = "Doe",
        type = NotificationType.LIKE, message = "John Doe liked your activity",
        referenceId = 42, referenceType = "ACTIVITY", read = false,
        createdAt = LocalDateTime.of(2026, 1, 2, 3, 4, 5)
    )

    @Test
    fun `GET notifications returns a plain list in the monolith DTO shape`() {
        whenever(notificationService.getNotifications(1)).thenReturn(listOf(dto()))

        mockMvc.perform(get("/api/v1/notifications").header("X-User-Id", "1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(5))
            .andExpect(jsonPath("$[0].senderFirstName").value("John"))
            .andExpect(jsonPath("$[0].senderLastName").value("Doe"))
            .andExpect(jsonPath("$[0].type").value("LIKE"))
            .andExpect(jsonPath("$[0].message").value("John Doe liked your activity"))
            .andExpect(jsonPath("$[0].referenceId").value(42))
            .andExpect(jsonPath("$[0].referenceType").value("ACTIVITY"))
            .andExpect(jsonPath("$[0].read").value(false))
            .andExpect(jsonPath("$[0].createdAt").value("2026-01-02T03:04:05"))
            // no Page envelope, no leaked fields
            .andExpect(jsonPath("$[0].content").doesNotExist())
            .andExpect(jsonPath("$[0].isRead").doesNotExist())
            .andExpect(jsonPath("$[0].recipientId").doesNotExist())
    }

    @Test
    fun `requests without gateway identity headers are rejected`() {
        mockMvc.perform(get("/api/v1/notifications"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `GET unread-count returns count envelope`() {
        whenever(notificationService.getUnreadCount(1)).thenReturn(7L)

        mockMvc.perform(get("/api/v1/notifications/unread-count").header("X-User-Id", "1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(7))
    }

    @Test
    fun `PUT read marks a notification read`() {
        mockMvc.perform(put("/api/v1/notifications/5/read").header("X-User-Id", "1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Notification marked as read"))

        verify(notificationService).markAsRead(5, 1)
    }

    @Test
    fun `PUT read-all marks everything read`() {
        mockMvc.perform(put("/api/v1/notifications/read-all").header("X-User-Id", "1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("All notifications marked as read"))

        verify(notificationService).markAllAsRead(1)
    }

    @Test
    fun `DELETE removes a single notification`() {
        mockMvc.perform(delete("/api/v1/notifications/5").header("X-User-Id", "1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Notification deleted"))

        verify(notificationService).deleteNotification(5, 1)
    }

    @Test
    fun `DELETE clears all notifications`() {
        mockMvc.perform(delete("/api/v1/notifications").header("X-User-Id", "1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("All notifications cleared"))

        verify(notificationService).clearAllNotifications(1)
    }

    @Test
    fun `unknown notification id maps to 404 ProblemDetail with message property`() {
        whenever(notificationService.markAsRead(99, 1)).thenThrow(NotFoundException("Notification not found"))

        mockMvc.perform(put("/api/v1/notifications/99/read").header("X-User-Id", "1"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Notification not found"))
            .andExpect(jsonPath("$.detail").value("Notification not found"))
    }

    @Test
    fun `foreign notification maps to 403 ProblemDetail with message property`() {
        whenever(notificationService.deleteNotification(5, 1)).thenThrow(ForbiddenException("Access denied"))

        mockMvc.perform(delete("/api/v1/notifications/5").header("X-User-Id", "1"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Access denied"))
    }

    @Test
    fun `weekly summary trigger requires ADMIN role`() {
        mockMvc.perform(
            post("/api/v1/notifications/weekly-summary/trigger")
                .header("X-User-Id", "1")
                .header("X-User-Roles", "USER")
        ).andExpect(status().isForbidden)

        verify(weeklySummaryService, never()).sendWeeklySummaries()
    }

    @Test
    fun `weekly summary trigger works for ADMIN`() {
        mockMvc.perform(
            post("/api/v1/notifications/weekly-summary/trigger")
                .header("X-User-Id", "4")
                .header("X-User-Roles", "ADMIN")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Weekly summary triggered"))

        verify(weeklySummaryService).sendWeeklySummaries()
    }

    @Test
    fun `GET stream opens an SSE connection for the authenticated user`() {
        whenever(sseEmitterService.createEmitter(1)).thenReturn(SseEmitter())

        mockMvc.perform(get("/api/v1/notifications/stream").header("X-User-Id", "1"))
            .andExpect(request().asyncStarted())

        verify(sseEmitterService).createEmitter(1)
    }

    @Test
    fun `GET stream without identity is rejected`() {
        mockMvc.perform(get("/api/v1/notifications/stream"))
            .andExpect(status().isForbidden)

        verify(sseEmitterService, never()).createEmitter(any())
    }
}
