package com.strava.notificationservice.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

/**
 * Notification-relevant slice of a user, served by user-service's internal
 * API (added in the user-service parity phase). Same field set as the
 * weekly-summary-recipients page rows.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class UserNotificationProfile(
    val id: Long = 0,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val fullName: String? = null,
    val avatar: String? = null,
    val emailNotifications: Boolean = false,
    val pushNotifications: Boolean = true,
    val weeklySummary: Boolean = false,
    val active: Boolean = true
)

/** Minimal projection of a Spring Data Page envelope. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class UserProfilePage(
    val content: List<UserNotificationProfile> = emptyList(),
    val last: Boolean = true
)

/**
 * Calls user-service via Spring Cloud LoadBalancer (lb://user-service)
 * resolved through Eureka, guarded by Resilience4j retry + circuit breaker
 * (same pattern as challenge-goal-service's ActivityClient). Fallbacks
 * return null — callers treat "profile unknown" as: persist + push the
 * notification, but skip email (we never email an address we can't verify).
 */
@Component
class UserProfileClient(
    private val loadBalanced: RestTemplate
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @CircuitBreaker(name = "user-service", fallbackMethod = "profileFallback")
    @Retry(name = "user-service")
    fun notificationProfile(userId: Long): UserNotificationProfile? =
        loadBalanced.getForObject(
            "lb://user-service/internal/users/$userId/notification-profile",
            UserNotificationProfile::class.java
        )

    @CircuitBreaker(name = "user-service", fallbackMethod = "recipientsFallback")
    @Retry(name = "user-service")
    fun weeklySummaryRecipients(page: Int, size: Int): UserProfilePage? =
        loadBalanced.getForObject(
            "lb://user-service/internal/users/weekly-summary-recipients?page=$page&size=$size",
            UserProfilePage::class.java
        )

    @Suppress("unused", "UNUSED_PARAMETER")
    private fun profileFallback(userId: Long, ex: Throwable): UserNotificationProfile? {
        log.warn("UserProfileClient fallback for userId={} (cause: {})", userId, ex.message)
        return null
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    private fun recipientsFallback(page: Int, size: Int, ex: Throwable): UserProfilePage? {
        log.warn("UserProfileClient recipients fallback page={} (cause: {})", page, ex.message)
        return null
    }
}
