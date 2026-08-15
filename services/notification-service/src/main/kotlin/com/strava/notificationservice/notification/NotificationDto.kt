package com.strava.notificationservice.notification

import com.strava.notificationservice.domain.Notification
import com.strava.notificationservice.domain.NotificationType
import java.time.LocalDateTime

/**
 * Serializes byte-for-byte like the monolith's NotificationDTO
 * (Lombok `boolean isRead` -> JSON field "read"; the frontend reads
 * `notification.read`, `senderFirstName`, `referenceId`, ...).
 */
data class NotificationDto(
    val id: Long?,
    val senderFirstName: String?,
    val senderLastName: String?,
    val type: NotificationType,
    val message: String,
    val referenceId: Long?,
    val referenceType: String?,
    val read: Boolean,
    val createdAt: LocalDateTime?
) {
    companion object {
        fun from(n: Notification) = NotificationDto(
            id = n.id,
            senderFirstName = n.senderFirstName,
            senderLastName = n.senderLastName,
            type = n.type,
            message = n.message,
            referenceId = n.referenceId,
            referenceType = n.referenceType,
            read = n.isRead,
            createdAt = n.createdAt
        )
    }
}
