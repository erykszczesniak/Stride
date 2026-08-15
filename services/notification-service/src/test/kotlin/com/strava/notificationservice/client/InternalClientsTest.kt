package com.strava.notificationservice.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime

/**
 * Happy-path URL contract for the lb:// internal calls. The Resilience4j
 * fallback wiring (annotations) is exercised at runtime via Spring AOP and
 * covered by the service-level "client returned null" tests.
 */
class InternalClientsTest {

    private val rest: RestTemplate = mock()

    @Test
    fun `notification profile is fetched from user-service internal API`() {
        val profile = UserNotificationProfile(id = 7, email = "u7@example.com")
        whenever(
            rest.getForObject(
                "lb://user-service/internal/users/7/notification-profile",
                UserNotificationProfile::class.java
            )
        ).thenReturn(profile)

        assertThat(UserProfileClient(rest).notificationProfile(7)).isEqualTo(profile)
    }

    @Test
    fun `weekly summary recipients are paged from user-service internal API`() {
        val page = UserProfilePage(content = listOf(UserNotificationProfile(id = 1)), last = true)
        whenever(
            rest.getForObject(
                "lb://user-service/internal/users/weekly-summary-recipients?page=0&size=200",
                UserProfilePage::class.java
            )
        ).thenReturn(page)

        assertThat(UserProfileClient(rest).weeklySummaryRecipients(0, 200)).isEqualTo(page)
    }

    @Test
    fun `range stats are fetched from activity-service internal API`() {
        val from = LocalDateTime.of(2026, 8, 3, 0, 0, 0)
        val to = LocalDateTime.of(2026, 8, 9, 23, 59, 59)
        val stats = ActivityRangeStats(12.5, 3600, 100.0, 500, 2)
        whenever(
            rest.getForObject(
                "lb://activity-service/internal/activities/stats/range?userId=7&from=2026-08-03T00:00:00&to=2026-08-09T23:59:59",
                ActivityRangeStats::class.java
            )
        ).thenReturn(stats)

        assertThat(ActivityStatsClient(rest).statsForRange(7, from, to)).isEqualTo(stats)
    }
}
