package com.strava.notificationservice.weekly

import com.strava.notificationservice.client.ActivityRangeStats
import com.strava.notificationservice.client.ActivityStatsClient
import com.strava.notificationservice.client.UserNotificationProfile
import com.strava.notificationservice.client.UserProfileClient
import com.strava.notificationservice.email.EmailNotificationService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import java.time.DayOfWeek
import java.time.LocalDateTime

class WeeklySummaryServiceTest {

    private lateinit var userProfiles: UserProfileClient
    private lateinit var activityStats: ActivityStatsClient
    private lateinit var emailService: EmailNotificationService
    private lateinit var templateEngine: TemplateEngine
    private lateinit var service: WeeklySummaryService

    @BeforeEach
    fun setUp() {
        userProfiles = mock()
        activityStats = mock()
        emailService = mock()
        templateEngine = mock()
        whenever(templateEngine.process(eq("mail/weekly-summary"), any<Context>())).thenReturn("<html/>")
        service = WeeklySummaryService(
            userProfiles, activityStats, emailService, templateEngine,
            "Stride", "http://localhost:3000,http://localhost:5173", 2
        )
    }

    private fun user(id: Long, weekly: Boolean = true, active: Boolean = true) = UserNotificationProfile(
        id = id, email = "user$id@example.com", firstName = "User$id", lastName = "Test",
        emailNotifications = true, pushNotifications = true, weeklySummary = weekly, active = active
    )

    private fun page(users: List<UserNotificationProfile>, last: Boolean) =
        com.strava.notificationservice.client.UserProfilePage(content = users, last = last)

    @Test
    fun `sends summaries to eligible recipients across pages`() {
        whenever(userProfiles.weeklySummaryRecipients(0, 2))
            .thenReturn(page(listOf(user(1), user(2, weekly = false)), last = false))
        whenever(userProfiles.weeklySummaryRecipients(1, 2))
            .thenReturn(page(listOf(user(3, active = false), user(4)), last = true))
        whenever(activityStats.statsForRange(any(), any(), any()))
            .thenReturn(ActivityRangeStats(12.5, 3720, 450.0, 900, 3))

        service.sendWeeklySummaries()

        // only users 1 and 4 are weeklySummary+active
        verify(emailService).sendWeeklySummaryEmail(eq(user(1)), any(), eq("<html/>"))
        verify(emailService).sendWeeklySummaryEmail(eq(user(4)), any(), eq("<html/>"))
        verify(emailService, never()).sendWeeklySummaryEmail(eq(user(2, weekly = false)), any(), any())
        verify(emailService, never()).sendWeeklySummaryEmail(eq(user(3, active = false)), any(), any())
    }

    @Test
    fun `subject and template variables mirror the monolith`() {
        whenever(userProfiles.weeklySummaryRecipients(0, 2))
            .thenReturn(page(listOf(user(1)), last = true))
        whenever(activityStats.statsForRange(any(), any(), any()))
            .thenReturn(ActivityRangeStats(12.5, 3720, 450.9, 900, 3))

        service.sendWeeklySummaries()

        val subjectCaptor = argumentCaptor<String>()
        verify(emailService).sendWeeklySummaryEmail(eq(user(1)), subjectCaptor.capture(), eq("<html/>"))
        assertThat(subjectCaptor.firstValue).startsWith("Stride - Your Weekly Training Summary (")

        val contextCaptor = argumentCaptor<Context>()
        verify(templateEngine).process(eq("mail/weekly-summary"), contextCaptor.capture())
        val ctx = contextCaptor.firstValue
        assertThat(ctx.getVariable("appName")).isEqualTo("Stride")
        assertThat(ctx.getVariable("firstName")).isEqualTo("User1")
        assertThat(ctx.getVariable("distance")).isEqualTo(String.format("%.1f", 12.5))
        assertThat(ctx.getVariable("hours")).isEqualTo("1")
        assertThat(ctx.getVariable("minutes")).isEqualTo("2")
        assertThat(ctx.getVariable("activities")).isEqualTo(3)
        assertThat(ctx.getVariable("calories")).isEqualTo(900)
        assertThat(ctx.getVariable("elevation")).isEqualTo(450)
        assertThat(ctx.getVariable("motivationMessage"))
            .isEqualTo("Keep up the great work! Every session counts.")
        assertThat(ctx.getVariable("settingsUrl"))
            .isEqualTo("http://localhost:3000/settings?section=notifications")
    }

    @Test
    fun `zero activities gets the motivational nudge`() {
        whenever(userProfiles.weeklySummaryRecipients(0, 2))
            .thenReturn(page(listOf(user(1)), last = true))
        whenever(activityStats.statsForRange(any(), any(), any()))
            .thenReturn(ActivityRangeStats(0.0, 0, 0.0, 0, 0))

        service.sendWeeklySummaries()

        val contextCaptor = argumentCaptor<Context>()
        verify(templateEngine).process(eq("mail/weekly-summary"), contextCaptor.capture())
        assertThat(contextCaptor.firstValue.getVariable("motivationMessage"))
            .isEqualTo("No activities recorded last week. Get out there and crush it this week!")
        verify(emailService).sendWeeklySummaryEmail(any(), any(), any())
    }

    @Test
    fun `stats window is last calendar week starting Monday`() {
        whenever(userProfiles.weeklySummaryRecipients(0, 2))
            .thenReturn(page(listOf(user(1)), last = true))
        whenever(activityStats.statsForRange(any(), any(), any()))
            .thenReturn(ActivityRangeStats(1.0, 60, 0.0, 10, 1))

        service.sendWeeklySummaries()

        val fromCaptor = argumentCaptor<LocalDateTime>()
        val toCaptor = argumentCaptor<LocalDateTime>()
        verify(activityStats).statsForRange(eq(1L), fromCaptor.capture(), toCaptor.capture())

        val from = fromCaptor.firstValue
        assertThat(from.dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
        assertThat(from.hour).isZero()
        assertThat(from.minute).isZero()
        assertThat(from.second).isZero()
        assertThat(toCaptor.firstValue).isEqualTo(from.plusWeeks(1).minusSeconds(1))
    }

    @Test
    fun `user with unavailable stats is skipped without failing the batch`() {
        whenever(userProfiles.weeklySummaryRecipients(0, 2))
            .thenReturn(page(listOf(user(1), user(2)), last = true))
        whenever(activityStats.statsForRange(eq(1L), any(), any())).thenReturn(null)
        whenever(activityStats.statsForRange(eq(2L), any(), any()))
            .thenReturn(ActivityRangeStats(5.0, 600, 0.0, 100, 1))

        service.sendWeeklySummaries()

        verify(emailService, never()).sendWeeklySummaryEmail(eq(user(1)), any(), any())
        verify(emailService).sendWeeklySummaryEmail(eq(user(2)), any(), any())
    }

    @Test
    fun `job aborts quietly when the recipients page cannot be fetched`() {
        whenever(userProfiles.weeklySummaryRecipients(0, 2)).thenReturn(null)

        service.sendWeeklySummaries()

        verifyNoInteractions(emailService)
        verifyNoInteractions(activityStats)
    }
}
