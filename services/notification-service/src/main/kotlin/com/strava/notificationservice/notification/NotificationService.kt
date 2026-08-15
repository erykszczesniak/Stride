package com.strava.notificationservice.notification

import com.strava.notificationservice.client.UserProfileClient
import com.strava.notificationservice.domain.Notification
import com.strava.notificationservice.domain.NotificationRepository
import com.strava.notificationservice.domain.NotificationType
import com.strava.notificationservice.email.EmailNotificationService
import com.strava.notificationservice.exception.ForbiddenException
import com.strava.notificationservice.exception.NotFoundException
import com.strava.notificationservice.sse.SseEmitterService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Port of the monolith's NotificationServiceImpl. Same read/write contract
 * and the same createNotification rules: no self-notifications, unread LIKE
 * dedup per (recipient, sender, referenceId), email gated on the recipient's
 * emailNotifications pref, SSE push gated on pushNotifications.
 *
 * The recipient's preferences live in user-service; we fetch them per
 * create via the internal notification-profile API (Resilience4j-guarded).
 * When that call fails we still persist + push the notification (users
 * should never lose an in-app notification because a peer service was
 * down) but skip the email — never send mail to an unverified address.
 */
@Service
class NotificationService(
    private val repo: NotificationRepository,
    private val emailService: EmailNotificationService,
    private val sse: SseEmitterService,
    private val userProfiles: UserProfileClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun getNotifications(userId: Long): List<NotificationDto> =
        repo.findByRecipientIdOrderByCreatedAtDesc(userId).map(NotificationDto::from)

    @Transactional(readOnly = true)
    fun getUnreadCount(userId: Long): Long = repo.countByRecipientIdAndIsReadFalse(userId)

    @Transactional
    fun markAsRead(notificationId: Long, userId: Long) {
        val notification = findOwn(notificationId, userId)
        notification.isRead = true
        repo.save(notification)
    }

    @Transactional
    fun markAllAsRead(userId: Long) {
        repo.markAllAsReadByRecipientId(userId)
    }

    @Transactional
    fun deleteNotification(notificationId: Long, userId: Long) {
        repo.delete(findOwn(notificationId, userId))
    }

    @Transactional
    fun clearAllNotifications(userId: Long) {
        repo.deleteAllByRecipientId(userId)
    }

    @Transactional
    fun createNotification(
        recipientId: Long,
        senderId: Long?,
        senderFirstName: String?,
        senderLastName: String?,
        type: NotificationType,
        message: String,
        referenceId: Long?,
        referenceType: String?
    ): Notification? {
        if (senderId != null && recipientId == senderId) {
            return null
        }

        if (type == NotificationType.LIKE && senderId != null && referenceId != null &&
            repo.existsByRecipientIdAndSenderIdAndTypeAndReferenceIdAndIsReadFalse(
                recipientId, senderId, type, referenceId
            )
        ) {
            return null
        }

        val saved = repo.save(
            Notification(
                recipientId = recipientId,
                senderId = senderId,
                senderFirstName = senderFirstName,
                senderLastName = senderLastName,
                type = type,
                message = message,
                referenceId = referenceId,
                referenceType = referenceType,
                isRead = false
            )
        )

        val profile = userProfiles.notificationProfile(recipientId)

        if (profile != null) {
            try {
                emailService.sendNotificationEmail(saved, profile)
            } catch (e: Exception) {
                log.warn("Failed to queue email notification: {}", e.message)
            }
        }

        // Push when the pref allows it — or when the profile is unavailable
        // (fail-open for the in-app channel, fail-closed for email).
        if (profile == null || profile.pushNotifications) {
            sse.sendNotification(recipientId, NotificationDto.from(saved))
        }

        return saved
    }

    private fun findOwn(notificationId: Long, userId: Long): Notification {
        val notification = repo.findById(notificationId)
            .orElseThrow { NotFoundException("Notification not found") }
        if (notification.recipientId != userId) {
            throw ForbiddenException("Access denied")
        }
        return notification
    }
}
