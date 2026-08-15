package com.strava.notificationservice.email

import com.strava.notificationservice.client.UserNotificationProfile
import com.strava.notificationservice.domain.Notification
import com.strava.notificationservice.domain.NotificationType
import jakarta.mail.MessagingException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context

/**
 * Port of the monolith's EmailNotificationServiceImpl. Subjects, icons,
 * action labels/URLs and the Thymeleaf templates are copied 1:1 from main.
 * The recipient (email address + preferences) comes from user-service's
 * internal notification-profile API instead of a local users table.
 * When the mail username is blank the send is skipped silently (parity).
 */
@Service
class EmailNotificationService(
    private val mailSender: JavaMailSender,
    private val templateEngine: TemplateEngine,
    @Value("\${spring.mail.username:}") private val fromAddress: String,
    @Value("\${app.name:Stride}") private val appName: String,
    @Value("\${app.cors.allowed-origins:http://localhost:3000}") private val allowedOrigins: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    fun sendNotificationEmail(notification: Notification, recipient: UserNotificationProfile) {
        if (!recipient.emailNotifications) {
            return
        }
        if (fromAddress.isBlank()) {
            log.warn("Mail username not configured, skipping email notification for user {}", recipient.id)
            return
        }
        val to = recipient.email
        if (to.isNullOrBlank()) {
            log.warn("No email address known for user {}, skipping email notification", recipient.id)
            return
        }

        sendHtmlEmail(to, getSubject(notification), buildHtmlBody(notification, recipient))
    }

    @Async
    fun sendWeeklySummaryEmail(recipient: UserNotificationProfile, subject: String, htmlBody: String) {
        if (fromAddress.isBlank()) {
            log.warn("Mail username not configured, skipping weekly summary for user {}", recipient.id)
            return
        }
        val to = recipient.email
        if (to.isNullOrBlank()) {
            log.warn("No email address known for user {}, skipping weekly summary", recipient.id)
            return
        }

        sendHtmlEmail(to, subject, htmlBody)
    }

    private fun sendHtmlEmail(to: String, subject: String, htmlBody: String) {
        try {
            val message = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")
            helper.setFrom(fromAddress)
            helper.setTo(to)
            helper.setSubject(subject)
            helper.setText(htmlBody, true)
            mailSender.send(message)
            log.info("Email sent to {}: {}", to, subject)
        } catch (e: MessagingException) {
            log.error("Failed to send email to {}: {}", to, e.message)
        } catch (e: Exception) {
            log.error("Unexpected error sending email to {}: {}", to, e.message)
        }
    }

    private fun getSubject(notification: Notification): String = when (notification.type) {
        NotificationType.LIKE -> "$appName - Someone liked your activity"
        NotificationType.COMMENT -> "$appName - New comment on your activity"
        NotificationType.FOLLOW -> "$appName - You have a new follower"
        NotificationType.ACHIEVEMENT -> "$appName - Achievement unlocked!"
        NotificationType.GOAL_COMPLETED -> "$appName - Goal completed!"
        NotificationType.SYSTEM -> "$appName - System notification"
    }

    private fun getFrontendUrl(): String = allowedOrigins.split(",")[0].trim()

    private fun buildActionUrl(notification: Notification): String {
        val base = getFrontendUrl()
        if (notification.referenceId == null) return base

        return when (notification.referenceType) {
            "ACTIVITY" -> "$base/activities/${notification.referenceId}"
            "GOAL" -> "$base/goals/${notification.referenceId}"
            else -> base
        }
    }

    private fun getActionLabel(notification: Notification): String = when (notification.type) {
        NotificationType.LIKE, NotificationType.COMMENT -> "View Activity"
        NotificationType.FOLLOW -> "View Profile"
        NotificationType.GOAL_COMPLETED -> "View Goal"
        else -> "Open $appName"
    }

    private fun getTypeIcon(notification: Notification): String = when (notification.type) {
        NotificationType.LIKE -> "&#10084;&#65039;"
        NotificationType.COMMENT -> "&#128172;"
        NotificationType.FOLLOW -> "&#128100;"
        NotificationType.ACHIEVEMENT -> "&#127942;"
        NotificationType.GOAL_COMPLETED -> "&#127919;"
        NotificationType.SYSTEM -> "&#128276;"
    }

    private fun buildHtmlBody(notification: Notification, recipient: UserNotificationProfile): String {
        val context = Context()
        context.setVariable("appName", appName)
        context.setVariable("icon", getTypeIcon(notification))
        context.setVariable("message", notification.message)
        context.setVariable("actionUrl", buildActionUrl(notification))
        context.setVariable("actionLabel", getActionLabel(notification))
        context.setVariable("recipientName", recipient.firstName)
        context.setVariable("settingsUrl", getFrontendUrl() + "/settings?section=notifications")

        return templateEngine.process("mail/notification", context)
    }
}
