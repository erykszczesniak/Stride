package com.strava.notificationservice.config

import com.strava.notificationservice.domain.NotificationType
import com.strava.notificationservice.notification.NotificationService
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * Only fires under the standalone profile (smoke testing without Kafka).
 * Seeds a couple of sample notifications so the REST endpoints have something
 * to return, since the consumers won't be invoked.
 */
@Component
@Profile("standalone")
class StandaloneSeeder(
    private val notifications: NotificationService
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(vararg args: String?) {
        log.info("[standalone] Seeding sample notifications")
        notifications.create(
            recipientUserId = 1,
            type = NotificationType.WELCOME,
            title = "Welcome to Stride!",
            body = "Get started by recording your first activity.",
            metadata = mapOf("source" to "seeder")
        )
        notifications.create(
            recipientUserId = 1,
            type = NotificationType.PR_ACHIEVED,
            title = "New personal record!",
            body = "RUN · 5 km in 20:00",
            metadata = mapOf("activityId" to "42", "sport" to "RUN")
        )
        notifications.create(
            recipientUserId = 2,
            type = NotificationType.WELCOME,
            title = "Welcome, Alice!",
            metadata = mapOf("source" to "seeder")
        )
    }
}
