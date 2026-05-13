package com.strava.notificationservice.notification

import com.strava.notificationservice.domain.Notification
import com.strava.notificationservice.domain.NotificationRepository
import com.strava.notificationservice.domain.NotificationType
import com.strava.notificationservice.exception.NotFoundException
import com.strava.notificationservice.sse.SseService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class NotificationService(
    private val repo: NotificationRepository,
    private val sse: SseService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun create(
        recipientUserId: Long,
        type: NotificationType,
        title: String,
        body: String? = null,
        metadata: Map<String, String> = emptyMap()
    ): NotificationDto {
        val notification = Notification(
            recipientUserId = recipientUserId,
            type = type,
            title = title,
            body = body,
            metadata = if (metadata.isEmpty()) null else encodeMetadata(metadata)
        )
        repo.save(notification)
        val dto = NotificationDto.from(notification)
        sse.push(recipientUserId, dto)
        log.debug("Created notification id={} type={} for userId={}",
            notification.id, type, recipientUserId)
        return dto
    }

    @Transactional(readOnly = true)
    fun list(userId: Long, pageable: Pageable): Page<NotificationDto> =
        repo.findAllByRecipientUserIdOrderByCreatedAtDesc(userId, pageable).map(NotificationDto::from)

    @Transactional(readOnly = true)
    fun unreadCount(userId: Long): Long = repo.countByRecipientUserIdAndReadFalse(userId)

    @Transactional
    fun markRead(notificationId: Long, userId: Long): NotificationDto {
        val n = repo.findById(notificationId)
            .orElseThrow { NotFoundException("Notification $notificationId not found") }
        if (n.recipientUserId != userId) {
            throw NotFoundException("Notification $notificationId not found")
        }
        if (!n.read) {
            n.read = true
            n.readAt = LocalDateTime.now()
        }
        return NotificationDto.from(n)
    }

    @Transactional
    fun markAllRead(userId: Long): Int = repo.markAllReadForUser(userId, LocalDateTime.now())

    /** Trivial JSON encoder; we never accept arbitrary characters here. */
    private fun encodeMetadata(map: Map<String, String>): String =
        map.entries.joinToString(
            prefix = "{", postfix = "}", separator = ","
        ) { (k, v) -> "\"${escape(k)}\":\"${escape(v)}\"" }

    private fun escape(s: String): String = s.replace("\\", "\\\\").replace("\"", "\\\"")
}
