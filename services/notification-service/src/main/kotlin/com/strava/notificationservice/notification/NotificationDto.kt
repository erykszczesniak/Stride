package com.strava.notificationservice.notification

import com.strava.notificationservice.domain.Notification
import com.strava.notificationservice.domain.NotificationType
import java.time.LocalDateTime

data class NotificationDto(
    val id: Long,
    val recipientUserId: Long,
    val type: NotificationType,
    val title: String,
    val body: String?,
    val metadata: String?,
    val read: Boolean,
    val readAt: LocalDateTime?,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(n: Notification) = NotificationDto(
            id = n.id!!,
            recipientUserId = n.recipientUserId,
            type = n.type,
            title = n.title,
            body = n.body,
            metadata = n.metadata,
            read = n.read,
            readAt = n.readAt,
            createdAt = n.createdAt
        )
    }
}
