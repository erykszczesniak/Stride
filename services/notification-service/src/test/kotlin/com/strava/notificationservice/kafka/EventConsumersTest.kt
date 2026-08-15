package com.strava.notificationservice.kafka

import com.strava.notificationservice.domain.NotificationType
import com.strava.notificationservice.notification.NotificationService
import com.strava.shared.events.ActivityLikedEvent
import com.strava.shared.events.CommentAddedEvent
import com.strava.shared.events.PersonalRecordAchievedEvent
import com.strava.shared.events.UserFollowedEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.time.Instant

class EventConsumersTest {

    private lateinit var notifications: NotificationService
    private lateinit var consumers: EventConsumers

    @BeforeEach
    fun setUp() {
        notifications = mock()
        consumers = EventConsumers(notifications)
    }

    @Test
    fun `user followed creates FOLLOW notification for followee with monolith wording`() {
        consumers.onUserFollowed(UserFollowedEvent(2L, 1L, "John Doe", Instant.now()))

        verify(notifications).createNotification(
            eq(1L),
            eq(2L),
            eq("John"),
            eq("Doe"),
            eq(NotificationType.FOLLOW),
            eq("John Doe started following you"),
            isNull(),
            isNull()
        )
    }

    @Test
    fun `single-word display name maps to firstName only`() {
        consumers.onUserFollowed(UserFollowedEvent(2L, 1L, "Cher", Instant.now()))

        verify(notifications).createNotification(
            eq(1L),
            eq(2L),
            eq("Cher"),
            isNull(),
            eq(NotificationType.FOLLOW),
            eq("Cher started following you"),
            isNull(),
            isNull()
        )
    }

    @Test
    fun `activity liked creates LIKE notification for owner referencing the activity`() {
        consumers.onActivityLiked(ActivityLikedEvent(42L, 1L, 2L, "John Doe", Instant.now()))

        verify(notifications).createNotification(
            eq(1L),
            eq(2L),
            eq("John"),
            eq("Doe"),
            eq(NotificationType.LIKE),
            eq("John Doe liked your activity"),
            eq(42L),
            eq("ACTIVITY")
        )
    }

    @Test
    fun `comment added creates COMMENT notification for owner referencing the activity`() {
        consumers.onCommentAdded(
            CommentAddedEvent(7L, 42L, 1L, 3L, "Jane Smith", "Nice run!", Instant.now())
        )

        verify(notifications).createNotification(
            eq(1L),
            eq(3L),
            eq("Jane"),
            eq("Smith"),
            eq(NotificationType.COMMENT),
            eq("Jane Smith commented on your activity"),
            eq(42L),
            eq("ACTIVITY")
        )
    }

    @Test
    fun `personal record creates ACHIEVEMENT notification referencing the activity`() {
        consumers.onPersonalRecordAchieved(
            PersonalRecordAchievedEvent(1L, 42L, "RUN", "FIVE_K", 1500L, Instant.now())
        )

        verify(notifications).createNotification(
            eq(1L),
            isNull(),
            isNull(),
            isNull(),
            eq(NotificationType.ACHIEVEMENT),
            eq("New personal record! RUN · five k in 25:00"),
            eq(42L),
            eq("ACTIVITY")
        )
    }

    @Test
    fun `personal record over an hour uses h-mm-ss format`() {
        consumers.onPersonalRecordAchieved(
            PersonalRecordAchievedEvent(1L, 42L, "RIDE", "HUNDRED_K", 3723L, Instant.now())
        )

        verify(notifications).createNotification(
            eq(1L),
            isNull(),
            isNull(),
            isNull(),
            eq(NotificationType.ACHIEVEMENT),
            eq("New personal record! RIDE · hundred k in 1:02:03"),
            eq(42L),
            eq("ACTIVITY")
        )
    }
}
