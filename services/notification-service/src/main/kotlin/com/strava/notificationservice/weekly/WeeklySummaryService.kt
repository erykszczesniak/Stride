package com.strava.notificationservice.weekly

import com.strava.notificationservice.client.ActivityStatsClient
import com.strava.notificationservice.client.UserNotificationProfile
import com.strava.notificationservice.client.UserProfileClient
import com.strava.notificationservice.email.EmailNotificationService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * Port of the monolith's WeeklySummaryServiceImpl (cron 9:00 Monday +
 * admin-triggered). Recipients are paged from user-service's internal
 * weekly-summary-recipients API; per-user totals come from activity-service's
 * internal stats/range API. Both calls are Resilience4j-guarded: a failed
 * recipients page aborts the job with a log line, a failed stats call skips
 * just that user — the job never crashes because a peer service is down.
 */
@Service
class WeeklySummaryService(
    private val userProfiles: UserProfileClient,
    private val activityStats: ActivityStatsClient,
    private val emailService: EmailNotificationService,
    private val templateEngine: TemplateEngine,
    @Value("\${app.name:Stride}") private val appName: String,
    @Value("\${app.cors.allowed-origins:http://localhost:3000}") private val allowedOrigins: String,
    @Value("\${app.weekly-summary.page-size:200}") private val pageSize: Int
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 9 ? * MON")
    fun sendWeeklySummaries() {
        log.info("Starting weekly summary email job")

        var sent = 0
        var page = 0
        while (true) {
            val recipients = userProfiles.weeklySummaryRecipients(page, pageSize)
            if (recipients == null) {
                log.error("Could not fetch weekly summary recipients (user-service unavailable), aborting job")
                break
            }

            for (user in recipients.content) {
                if (!user.weeklySummary || !user.active) {
                    continue
                }
                try {
                    if (sendSummaryForUser(user)) {
                        sent++
                    }
                } catch (e: Exception) {
                    log.error("Failed to send weekly summary for user {}: {}", user.id, e.message)
                }
            }

            if (recipients.last || recipients.content.isEmpty()) {
                break
            }
            page++
        }

        log.info("Weekly summary job completed. Sent {} emails.", sent)
    }

    private fun sendSummaryForUser(user: UserNotificationProfile): Boolean {
        val now = LocalDateTime.now()
        val startOfLastWeek = now.with(TemporalAdjusters.previous(DayOfWeek.MONDAY))
            .minusWeeks(1)
            .withHour(0).withMinute(0).withSecond(0).withNano(0)
        val endOfLastWeek = startOfLastWeek.plusWeeks(1).minusSeconds(1)

        val stats = activityStats.statsForRange(user.id, startOfLastWeek, endOfLastWeek)
        if (stats == null) {
            log.warn("No activity stats available for user {} (activity-service unavailable), skipping", user.id)
            return false
        }

        val weekLabel = startOfLastWeek.format(DateTimeFormatter.ofPattern("MMM d")) +
            " - " + endOfLastWeek.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))

        val subject = "$appName - Your Weekly Training Summary ($weekLabel)"

        val context = Context()
        context.setVariable("appName", appName)
        context.setVariable("firstName", user.firstName)
        context.setVariable("weekLabel", weekLabel)
        context.setVariable("distance", String.format("%.1f", stats.totalDistanceKm))
        context.setVariable("hours", (stats.totalDurationSeconds / 3600).toString())
        context.setVariable("minutes", ((stats.totalDurationSeconds % 3600) / 60).toString())
        context.setVariable("activities", stats.activityCount)
        context.setVariable("calories", stats.totalCalories)
        context.setVariable("elevation", stats.totalElevationGain.toInt())
        context.setVariable(
            "motivationMessage",
            if (stats.activityCount > 0) "Keep up the great work! Every session counts."
            else "No activities recorded last week. Get out there and crush it this week!"
        )
        context.setVariable("settingsUrl", allowedOrigins.split(",")[0].trim() + "/settings?section=notifications")

        val htmlBody = templateEngine.process("mail/weekly-summary", context)

        emailService.sendWeeklySummaryEmail(user, subject, htmlBody)
        return true
    }
}
