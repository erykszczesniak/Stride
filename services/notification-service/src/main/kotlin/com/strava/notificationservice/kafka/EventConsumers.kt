package com.strava.notificationservice.kafka

import com.strava.notificationservice.domain.NotificationType
import com.strava.notificationservice.notification.NotificationService
import com.strava.shared.events.PersonalRecordAchievedEvent
import com.strava.shared.events.Topics
import com.strava.shared.events.UserRegisteredEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * Kafka consumers that translate domain events into user-facing notifications.
 *
 * Acknowledgement is automatic (Spring Kafka default) — at-least-once delivery.
 * If a notification insert fails we let the exception bubble so the consumer
 * retries; for production we'd add a DLQ + idempotency key (event id) to make
 * the insert safely retriable. That's part of the saga/observability phase.
 */
@Component
class EventConsumers(
    private val notifications: NotificationService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = [Topics.USER_REGISTERED], groupId = "notification-service")
    fun onUserRegistered(event: UserRegisteredEvent) {
        log.info("Consuming UserRegisteredEvent userId={}", event.userId())
        notifications.create(
            recipientUserId = event.userId(),
            type = NotificationType.WELCOME,
            title = "Welcome to Stride, ${event.displayName()}!",
            body = "Start by recording your first activity — we'll celebrate every personal record.",
            metadata = mapOf("source" to "stride.user.registered")
        )
    }

    @KafkaListener(topics = [Topics.PERSONAL_RECORD_ACHIEVED], groupId = "notification-service")
    fun onPersonalRecordAchieved(event: PersonalRecordAchievedEvent) {
        log.info("Consuming PersonalRecordAchievedEvent userId={} sport={} distance={} time={}s",
            event.userId(), event.sport(), event.distanceLabel(), event.timeSeconds())
        notifications.create(
            recipientUserId = event.userId(),
            type = NotificationType.PR_ACHIEVED,
            title = "New personal record!",
            body = "${event.sport()} · ${event.distanceLabel().lowercase().replace('_', ' ')} in ${formatTime(event.timeSeconds())}",
            metadata = mapOf(
                "activityId" to event.activityId().toString(),
                "sport" to event.sport(),
                "distance" to event.distanceLabel()
            )
        )
    }

    private fun formatTime(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }
}
