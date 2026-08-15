package com.strava.notificationservice.config

import com.strava.notificationservice.domain.Notification
import com.strava.notificationservice.domain.NotificationRepository
import com.strava.notificationservice.domain.NotificationType
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Only fires under the standalone profile (smoke testing without Kafka).
 * Seeds sample notifications in the monolith's shape (main's
 * V7__add_notifications.sql style) so the REST endpoints have something to
 * return. Uses the repository directly — no SSE push / email / profile
 * lookup side effects at boot.
 */
@Component
@Profile("standalone")
class StandaloneSeeder(
    private val repo: NotificationRepository
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(vararg args: String?) {
        if (repo.count() > 0) {
            log.info("[standalone] Notifications already present, skipping seed")
            return
        }
        log.info("[standalone] Seeding sample notifications")
        repo.saveAll(
            listOf(
                Notification(
                    recipientId = 1, senderId = 2, senderFirstName = "John", senderLastName = "Doe",
                    type = NotificationType.LIKE,
                    message = "John Doe liked your activity",
                    referenceId = 1, referenceType = "ACTIVITY",
                    isRead = false, createdAt = LocalDateTime.now().minusHours(2)
                ),
                Notification(
                    recipientId = 1, senderId = 3, senderFirstName = "Jane", senderLastName = "Smith",
                    type = NotificationType.COMMENT,
                    message = "Jane Smith commented on your activity",
                    referenceId = 1, referenceType = "ACTIVITY",
                    isRead = false, createdAt = LocalDateTime.now().minusHours(5)
                ),
                Notification(
                    recipientId = 1, senderId = 3, senderFirstName = "Jane", senderLastName = "Smith",
                    type = NotificationType.FOLLOW,
                    message = "Jane Smith started following you",
                    isRead = true, createdAt = LocalDateTime.now().minusDays(2)
                ),
                Notification(
                    recipientId = 1,
                    type = NotificationType.ACHIEVEMENT,
                    message = "New personal record! RUN · 5 km in 20:00",
                    referenceId = 42, referenceType = "ACTIVITY",
                    isRead = false, createdAt = LocalDateTime.now().minusHours(6)
                ),
                Notification(
                    recipientId = 2, senderId = 1, senderFirstName = "Pan", senderLastName = "Szczesniak",
                    type = NotificationType.LIKE,
                    message = "Pan Szczesniak liked your activity",
                    referenceId = 7, referenceType = "ACTIVITY",
                    isRead = false, createdAt = LocalDateTime.now().minusHours(1)
                ),
                Notification(
                    recipientId = 2,
                    type = NotificationType.SYSTEM,
                    message = "System maintenance scheduled for this weekend.",
                    isRead = true, createdAt = LocalDateTime.now().minusDays(1)
                )
            )
        )
    }
}
