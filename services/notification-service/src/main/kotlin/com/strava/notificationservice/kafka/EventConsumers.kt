package com.strava.notificationservice.kafka

import com.strava.notificationservice.domain.NotificationType
import com.strava.notificationservice.notification.NotificationService
import com.strava.shared.events.ActivityLikedEvent
import com.strava.shared.events.CommentAddedEvent
import com.strava.shared.events.PersonalRecordAchievedEvent
import com.strava.shared.events.Topics
import com.strava.shared.events.UserFollowedEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * Kafka consumers translating domain events into user-facing notifications,
 * mirroring the monolith's synchronous createNotification call sites
 * (UserServiceImpl.followUser, ActivityServiceImpl.likeActivity,
 * CommentServiceImpl.createComment). Self-notifications and duplicate
 * unread LIKEs are suppressed inside NotificationService — same rules as
 * main.
 *
 * Acknowledgement is automatic (Spring Kafka default) — at-least-once
 * delivery. The unread-LIKE dedup doubles as a light idempotency guard for
 * redelivered like events.
 */
@Component
class EventConsumers(
    private val notifications: NotificationService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = [Topics.USER_FOLLOWED], groupId = "notification-service")
    fun onUserFollowed(event: UserFollowedEvent) {
        log.info("Consuming UserFollowedEvent follower={} followee={}", event.followerUserId(), event.followeeUserId())
        notifications.createNotification(
            recipientId = event.followeeUserId(),
            senderId = event.followerUserId(),
            senderFirstName = firstNameOf(event.followerDisplayName()),
            senderLastName = lastNameOf(event.followerDisplayName()),
            type = NotificationType.FOLLOW,
            message = "${event.followerDisplayName()} started following you",
            referenceId = null,
            referenceType = null
        )
    }

    @KafkaListener(topics = [Topics.ACTIVITY_LIKED], groupId = "notification-service")
    fun onActivityLiked(event: ActivityLikedEvent) {
        log.info("Consuming ActivityLikedEvent activityId={} liker={}", event.activityId(), event.likerUserId())
        notifications.createNotification(
            recipientId = event.activityOwnerId(),
            senderId = event.likerUserId(),
            senderFirstName = firstNameOf(event.likerDisplayName()),
            senderLastName = lastNameOf(event.likerDisplayName()),
            type = NotificationType.LIKE,
            message = "${event.likerDisplayName()} liked your activity",
            referenceId = event.activityId(),
            referenceType = "ACTIVITY"
        )
    }

    @KafkaListener(topics = [Topics.COMMENT_ADDED], groupId = "notification-service")
    fun onCommentAdded(event: CommentAddedEvent) {
        log.info("Consuming CommentAddedEvent activityId={} author={}", event.activityId(), event.authorUserId())
        notifications.createNotification(
            recipientId = event.activityOwnerId(),
            senderId = event.authorUserId(),
            senderFirstName = firstNameOf(event.authorDisplayName()),
            senderLastName = lastNameOf(event.authorDisplayName()),
            type = NotificationType.COMMENT,
            message = "${event.authorDisplayName()} commented on your activity",
            referenceId = event.activityId(),
            referenceType = "ACTIVITY"
        )
    }

    @KafkaListener(topics = [Topics.PERSONAL_RECORD_ACHIEVED], groupId = "notification-service")
    fun onPersonalRecordAchieved(event: PersonalRecordAchievedEvent) {
        log.info(
            "Consuming PersonalRecordAchievedEvent userId={} sport={} distance={} time={}s",
            event.userId(), event.sport(), event.distanceLabel(), event.timeSeconds()
        )
        val distance = event.distanceLabel().lowercase().replace('_', ' ')
        notifications.createNotification(
            recipientId = event.userId(),
            senderId = null,
            senderFirstName = null,
            senderLastName = null,
            type = NotificationType.ACHIEVEMENT,
            message = "New personal record! ${event.sport()} · $distance in ${formatTime(event.timeSeconds())}",
            referenceId = event.activityId(),
            referenceType = "ACTIVITY"
        )
    }

    private fun firstNameOf(displayName: String?): String? =
        displayName?.trim()?.substringBefore(' ')?.ifBlank { null }

    private fun lastNameOf(displayName: String?): String? =
        displayName?.trim()?.substringAfter(' ', "")?.ifBlank { null }

    private fun formatTime(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }
}
