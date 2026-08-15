package com.strava.notificationservice.email

import com.strava.notificationservice.client.UserNotificationProfile
import com.strava.notificationservice.domain.Notification
import com.strava.notificationservice.domain.NotificationType
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.mail.javamail.JavaMailSender
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import java.util.Properties

class EmailNotificationServiceTest {

    private lateinit var mailSender: JavaMailSender
    private lateinit var templateEngine: TemplateEngine
    private lateinit var mimeMessage: MimeMessage

    @BeforeEach
    fun setUp() {
        mailSender = mock()
        templateEngine = mock()
        mimeMessage = MimeMessage(Session.getInstance(Properties()))
        whenever(mailSender.createMimeMessage()).thenReturn(mimeMessage)
        whenever(templateEngine.process(any<String>(), any<Context>())).thenReturn("<html/>")
    }

    private fun service(fromAddress: String = "noreply@stride.app") = EmailNotificationService(
        mailSender, templateEngine, fromAddress, "Stride", "http://localhost:3000,http://localhost:5173"
    )

    private fun recipient(emailNotifications: Boolean = true) = UserNotificationProfile(
        id = 1, email = "erik@example.com", firstName = "Erik", lastName = "S",
        emailNotifications = emailNotifications, pushNotifications = true
    )

    private fun notification(type: NotificationType = NotificationType.LIKE) = Notification(
        id = 5, recipientId = 1, senderId = 2, senderFirstName = "John", senderLastName = "Doe",
        type = type, message = "John Doe liked your activity",
        referenceId = 42, referenceType = "ACTIVITY"
    )

    @Test
    fun `skips silently when recipient disabled email notifications`() {
        service().sendNotificationEmail(notification(), recipient(emailNotifications = false))

        verifyNoInteractions(mailSender, templateEngine)
    }

    @Test
    fun `skips silently when mail username is blank`() {
        service(fromAddress = "").sendNotificationEmail(notification(), recipient())

        verifyNoInteractions(mailSender)
    }

    @Test
    fun `sends LIKE email with monolith subject and template variables`() {
        service().sendNotificationEmail(notification(), recipient())

        verify(mailSender).send(mimeMessage)
        assertThat(mimeMessage.subject).isEqualTo("Stride - Someone liked your activity")
        assertThat(mimeMessage.getRecipients(Message.RecipientType.TO)[0].toString())
            .isEqualTo("erik@example.com")
        assertThat(mimeMessage.from[0].toString()).isEqualTo("noreply@stride.app")

        val contextCaptor = argumentCaptor<Context>()
        verify(templateEngine).process(eq("mail/notification"), contextCaptor.capture())
        val ctx = contextCaptor.firstValue
        assertThat(ctx.getVariable("appName")).isEqualTo("Stride")
        assertThat(ctx.getVariable("icon")).isEqualTo("&#10084;&#65039;")
        assertThat(ctx.getVariable("message")).isEqualTo("John Doe liked your activity")
        assertThat(ctx.getVariable("actionUrl")).isEqualTo("http://localhost:3000/activities/42")
        assertThat(ctx.getVariable("actionLabel")).isEqualTo("View Activity")
        assertThat(ctx.getVariable("recipientName")).isEqualTo("Erik")
        assertThat(ctx.getVariable("settingsUrl"))
            .isEqualTo("http://localhost:3000/settings?section=notifications")
    }

    @Test
    fun `subjects and labels per type match the monolith`() {
        val svc = service()

        svc.sendNotificationEmail(notification(NotificationType.FOLLOW).apply {
            referenceId = null; referenceType = null
        }, recipient())
        assertThat(mimeMessage.subject).isEqualTo("Stride - You have a new follower")

        val contextCaptor = argumentCaptor<Context>()
        verify(templateEngine).process(eq("mail/notification"), contextCaptor.capture())
        assertThat(contextCaptor.firstValue.getVariable("actionLabel")).isEqualTo("View Profile")
        // no reference -> action url falls back to the frontend base
        assertThat(contextCaptor.firstValue.getVariable("actionUrl")).isEqualTo("http://localhost:3000")
    }

    @Test
    fun `GOAL reference builds a goal action url`() {
        service().sendNotificationEmail(notification(NotificationType.GOAL_COMPLETED).apply {
            referenceId = 9; referenceType = "GOAL"
        }, recipient())

        assertThat(mimeMessage.subject).isEqualTo("Stride - Goal completed!")
        val contextCaptor = argumentCaptor<Context>()
        verify(templateEngine).process(eq("mail/notification"), contextCaptor.capture())
        assertThat(contextCaptor.firstValue.getVariable("actionUrl")).isEqualTo("http://localhost:3000/goals/9")
        assertThat(contextCaptor.firstValue.getVariable("actionLabel")).isEqualTo("View Goal")
    }

    @Test
    fun `weekly summary email sends the prepared html`() {
        service().sendWeeklySummaryEmail(recipient(), "Stride - Your Weekly Training Summary", "<html/>")

        verify(mailSender).send(mimeMessage)
        assertThat(mimeMessage.subject).isEqualTo("Stride - Your Weekly Training Summary")
    }

    @Test
    fun `weekly summary skipped when mail username is blank`() {
        service(fromAddress = "").sendWeeklySummaryEmail(recipient(), "s", "<html/>")

        verifyNoInteractions(mailSender)
    }
}
