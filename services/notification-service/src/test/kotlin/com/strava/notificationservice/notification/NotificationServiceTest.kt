package com.strava.notificationservice.notification

import com.strava.notificationservice.client.UserNotificationProfile
import com.strava.notificationservice.client.UserProfileClient
import com.strava.notificationservice.domain.Notification
import com.strava.notificationservice.domain.NotificationRepository
import com.strava.notificationservice.domain.NotificationType
import com.strava.notificationservice.email.EmailNotificationService
import com.strava.notificationservice.exception.ForbiddenException
import com.strava.notificationservice.exception.NotFoundException
import com.strava.notificationservice.sse.SseEmitterService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.Optional

class NotificationServiceTest {

    private lateinit var repo: NotificationRepository
    private lateinit var emailService: EmailNotificationService
    private lateinit var sse: SseEmitterService
    private lateinit var userProfiles: UserProfileClient
    private lateinit var service: NotificationService

    @BeforeEach
    fun setUp() {
        repo = mock()
        emailService = mock()
        sse = mock()
        userProfiles = mock()
        service = NotificationService(repo, emailService, sse, userProfiles)

        // save() echoes the entity back with an id, like JPA would
        whenever(repo.save(any<Notification>())).doAnswer {
            val n = it.arguments[0] as Notification
            if (n.id == null) n.id = 99L
            n
        }
    }

    private fun notification(
        id: Long = 5,
        recipientId: Long = 1,
        senderId: Long? = 2,
        isRead: Boolean = false
    ) = Notification(
        id = id,
        recipientId = recipientId,
        senderId = senderId,
        senderFirstName = "John",
        senderLastName = "Doe",
        type = NotificationType.LIKE,
        message = "John Doe liked your activity",
        referenceId = 42,
        referenceType = "ACTIVITY",
        isRead = isRead,
        createdAt = LocalDateTime.of(2026, 1, 2, 3, 4, 5)
    )

    private fun profile(email: Boolean = true, push: Boolean = true) = UserNotificationProfile(
        id = 1, email = "erik@example.com", firstName = "Erik", lastName = "S",
        emailNotifications = email, pushNotifications = push, weeklySummary = true, active = true
    )

    @Test
    fun `getNotifications maps entities to monolith-shaped DTOs`() {
        whenever(repo.findByRecipientIdOrderByCreatedAtDesc(1)).thenReturn(listOf(notification()))

        val result = service.getNotifications(1)

        assertThat(result).hasSize(1)
        val dto = result[0]
        assertThat(dto.id).isEqualTo(5)
        assertThat(dto.senderFirstName).isEqualTo("John")
        assertThat(dto.senderLastName).isEqualTo("Doe")
        assertThat(dto.type).isEqualTo(NotificationType.LIKE)
        assertThat(dto.message).isEqualTo("John Doe liked your activity")
        assertThat(dto.referenceId).isEqualTo(42)
        assertThat(dto.referenceType).isEqualTo("ACTIVITY")
        assertThat(dto.read).isFalse()
        assertThat(dto.createdAt).isEqualTo(LocalDateTime.of(2026, 1, 2, 3, 4, 5))
    }

    @Test
    fun `getUnreadCount delegates to repository`() {
        whenever(repo.countByRecipientIdAndIsReadFalse(1)).thenReturn(7)

        assertThat(service.getUnreadCount(1)).isEqualTo(7)
    }

    @Test
    fun `markAsRead throws NotFoundException for unknown id`() {
        whenever(repo.findById(5)).thenReturn(Optional.empty())

        assertThatThrownBy { service.markAsRead(5, 1) }
            .isInstanceOf(NotFoundException::class.java)
            .hasMessage("Notification not found")
    }

    @Test
    fun `markAsRead throws ForbiddenException for another user's notification`() {
        whenever(repo.findById(5)).thenReturn(Optional.of(notification(recipientId = 8)))

        assertThatThrownBy { service.markAsRead(5, 1) }
            .isInstanceOf(ForbiddenException::class.java)
            .hasMessage("Access denied")
    }

    @Test
    fun `markAsRead flips the flag and saves`() {
        val n = notification()
        whenever(repo.findById(5)).thenReturn(Optional.of(n))

        service.markAsRead(5, 1)

        assertThat(n.isRead).isTrue()
        verify(repo).save(n)
    }

    @Test
    fun `markAllAsRead delegates to bulk update`() {
        service.markAllAsRead(1)

        verify(repo).markAllAsReadByRecipientId(1)
    }

    @Test
    fun `deleteNotification checks ownership then deletes`() {
        val n = notification()
        whenever(repo.findById(5)).thenReturn(Optional.of(n))

        service.deleteNotification(5, 1)

        verify(repo).delete(n)
    }

    @Test
    fun `deleteNotification of another user's notification is forbidden`() {
        whenever(repo.findById(5)).thenReturn(Optional.of(notification(recipientId = 8)))

        assertThatThrownBy { service.deleteNotification(5, 1) }
            .isInstanceOf(ForbiddenException::class.java)
    }

    @Test
    fun `clearAllNotifications delegates to bulk delete`() {
        service.clearAllNotifications(1)

        verify(repo).deleteAllByRecipientId(1)
    }

    @Test
    fun `createNotification suppresses self-notifications`() {
        val result = service.createNotification(
            recipientId = 1, senderId = 1, senderFirstName = "Me", senderLastName = "Too",
            type = NotificationType.LIKE, message = "x", referenceId = 42, referenceType = "ACTIVITY"
        )

        assertThat(result).isNull()
        verify(repo, never()).save(any<Notification>())
        verifyNoInteractions(sse, emailService)
    }

    @Test
    fun `createNotification suppresses duplicate unread LIKE for same recipient sender reference`() {
        whenever(
            repo.existsByRecipientIdAndSenderIdAndTypeAndReferenceIdAndIsReadFalse(
                1, 2, NotificationType.LIKE, 42
            )
        ).thenReturn(true)

        val result = service.createNotification(
            recipientId = 1, senderId = 2, senderFirstName = "John", senderLastName = "Doe",
            type = NotificationType.LIKE, message = "John Doe liked your activity",
            referenceId = 42, referenceType = "ACTIVITY"
        )

        assertThat(result).isNull()
        verify(repo, never()).save(any<Notification>())
    }

    @Test
    fun `duplicate rule does not apply to COMMENT notifications`() {
        whenever(userProfiles.notificationProfile(1)).thenReturn(profile())

        val result = service.createNotification(
            recipientId = 1, senderId = 2, senderFirstName = "John", senderLastName = "Doe",
            type = NotificationType.COMMENT, message = "John Doe commented on your activity",
            referenceId = 42, referenceType = "ACTIVITY"
        )

        assertThat(result).isNotNull
        verify(repo, never()).existsByRecipientIdAndSenderIdAndTypeAndReferenceIdAndIsReadFalse(
            any(), any(), any(), any()
        )
        verify(repo).save(any<Notification>())
    }

    @Test
    fun `createNotification persists emails and pushes when prefs allow`() {
        whenever(userProfiles.notificationProfile(1)).thenReturn(profile(email = true, push = true))

        val result = service.createNotification(
            recipientId = 1, senderId = 2, senderFirstName = "John", senderLastName = "Doe",
            type = NotificationType.LIKE, message = "John Doe liked your activity",
            referenceId = 42, referenceType = "ACTIVITY"
        )

        assertThat(result).isNotNull
        assertThat(result!!.recipientId).isEqualTo(1)
        assertThat(result.isRead).isFalse()

        verify(emailService).sendNotificationEmail(eq(result), any())

        val dtoCaptor = argumentCaptor<NotificationDto>()
        verify(sse).sendNotification(eq(1L), dtoCaptor.capture())
        assertThat(dtoCaptor.firstValue.message).isEqualTo("John Doe liked your activity")
        assertThat(dtoCaptor.firstValue.read).isFalse()
    }

    @Test
    fun `push is skipped when pushNotifications is disabled`() {
        whenever(userProfiles.notificationProfile(1)).thenReturn(profile(email = true, push = false))

        service.createNotification(
            recipientId = 1, senderId = 2, senderFirstName = "John", senderLastName = "Doe",
            type = NotificationType.FOLLOW, message = "John Doe started following you",
            referenceId = null, referenceType = null
        )

        verify(emailService).sendNotificationEmail(any(), any())
        verifyNoInteractions(sse)
    }

    @Test
    fun `profile fetch failure still persists and pushes but skips email`() {
        whenever(userProfiles.notificationProfile(1)).thenReturn(null)

        val result = service.createNotification(
            recipientId = 1, senderId = 2, senderFirstName = "John", senderLastName = "Doe",
            type = NotificationType.COMMENT, message = "John Doe commented on your activity",
            referenceId = 42, referenceType = "ACTIVITY"
        )

        assertThat(result).isNotNull
        verify(repo).save(any<Notification>())
        verify(sse).sendNotification(eq(1L), any())
        verifyNoInteractions(emailService)
    }

    @Test
    fun `email failure does not fail notification creation`() {
        whenever(userProfiles.notificationProfile(1)).thenReturn(profile())
        whenever(emailService.sendNotificationEmail(any(), any())).thenThrow(RuntimeException("smtp down"))

        val result = service.createNotification(
            recipientId = 1, senderId = 2, senderFirstName = "John", senderLastName = "Doe",
            type = NotificationType.LIKE, message = "John Doe liked your activity",
            referenceId = 42, referenceType = "ACTIVITY"
        )

        assertThat(result).isNotNull
        verify(sse).sendNotification(eq(1L), any())
    }
}
