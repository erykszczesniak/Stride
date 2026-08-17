package com.strava.notificationservice.client

import com.strava.grpc.user.GetNotificationProfileRequest
import com.strava.grpc.user.ListWeeklySummaryRecipientsRequest
import com.strava.grpc.user.NotificationProfile
import com.strava.grpc.user.UserInternalServiceGrpc
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import io.grpc.Status
import io.grpc.StatusRuntimeException
import net.devh.boot.grpc.client.inject.GrpcClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * Notification-relevant slice of a user. Same field set the retired REST
 * /internal/users API served — now mapped from the
 * {@code stride.internal.v1.NotificationProfile} gRPC message, so downstream
 * code (email/push gating, weekly job) is unchanged by the transport swap.
 */
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

/**
 * Minimal page projection — only what the weekly job consumes. Populated from
 * the gRPC page envelope (recipients + totalPages) with the same "last" rule
 * as the Spring Data Page JSON it replaces.
 */
data class UserProfilePage(
    val content: List<UserNotificationProfile> = emptyList(),
    val last: Boolean = true
)

/**
 * Calls user-service over the cluster-internal gRPC surface
 * ({@code stride.internal.v1.UserInternalService}, resolved from Eureka via
 * {@code discovery:///user-service}), guarded by the same Resilience4j retry +
 * circuit breaker instances as the retired REST client. Fallbacks return
 * null — callers treat "profile unknown" as: persist + push the notification,
 * but skip email (we never email an address we can't verify).
 *
 * NOT_FOUND is mapped to null in-line (the REST predecessor yielded null for a
 * 404 too, via its fallback): a missing user is a data fact, not an outage, so
 * it no longer counts toward the circuit breaker's failure rate.
 */
@Component
class UserProfileClient {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Injected by net.devh from {@code grpc.client.user-service.*}. Internal +
     * lateinit: unit tests point it at an in-process server instead of
     * Mockito-mocking generated code (same seam as user-service's
     * ActivityStatsPortAdapter).
     */
    @field:GrpcClient("user-service")
    internal lateinit var userInternalStub: UserInternalServiceGrpc.UserInternalServiceBlockingStub

    @CircuitBreaker(name = "user-service", fallbackMethod = "profileFallback")
    @Retry(name = "user-service")
    fun notificationProfile(userId: Long): UserNotificationProfile? =
        try {
            userInternalStub
                .withDeadlineAfter(CALL_DEADLINE_SECONDS, TimeUnit.SECONDS)
                .getNotificationProfile(
                    GetNotificationProfileRequest.newBuilder().setUserId(userId).build()
                )
                .toDto()
        } catch (e: StatusRuntimeException) {
            if (e.status.code == Status.Code.NOT_FOUND) {
                log.warn("No notification profile for userId={} (NOT_FOUND)", userId)
                null
            } else {
                throw e
            }
        }

    @CircuitBreaker(name = "user-service", fallbackMethod = "recipientsFallback")
    @Retry(name = "user-service")
    fun weeklySummaryRecipients(page: Int, size: Int): UserProfilePage? {
        val response = userInternalStub
            .withDeadlineAfter(CALL_DEADLINE_SECONDS, TimeUnit.SECONDS)
            .listWeeklySummaryRecipients(
                ListWeeklySummaryRecipientsRequest.newBuilder().setPage(page).setSize(size).build()
            )
        return UserProfilePage(
            content = response.recipientsList.map { it.toDto() },
            // Same rule as Spring's Page.isLast(); an empty result (totalPages == 0) is last.
            last = page >= response.totalPages - 1
        )
    }

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

    /** proto3 optionals → nullable fields; a blank email means "no address known". */
    private fun NotificationProfile.toDto() = UserNotificationProfile(
        id = id,
        email = email.takeIf { it.isNotEmpty() },
        firstName = if (hasFirstName()) firstName else null,
        lastName = if (hasLastName()) lastName else null,
        fullName = if (hasFullName()) fullName else null,
        avatar = if (hasAvatar()) avatar else null,
        emailNotifications = emailNotifications,
        pushNotifications = pushNotifications,
        weeklySummary = weeklySummary,
        active = active
    )

    companion object {
        /** Deadlines are absolute, so the ~3s budget is applied per call, never on the injected stub. */
        private const val CALL_DEADLINE_SECONDS = 3L
    }
}
