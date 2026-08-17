package com.strava.aiservice.client

import com.strava.aiservice.exception.ApiRequestException
import com.strava.grpc.activity.ActivityInternalServiceGrpc
import com.strava.grpc.activity.ActivityPayload
import com.strava.grpc.activity.ActivitySummaryItem
import com.strava.grpc.activity.GetActivitiesRequest
import com.strava.grpc.activity.ListRecentActivitiesRequest
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import net.devh.boot.grpc.client.inject.GrpcClient
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Fetches the activity backing a chat from activity-service's cluster-internal
 * gRPC API (ActivityInternalService.GetActivities), resolved through Eureka
 * (discovery:///activity-service) by the net.devh starter and wrapped in
 * Resilience4j retry + circuit breaker (instance "activity-service";
 * StatusRuntimeException is in the recordExceptions list so failed RPCs count
 * toward opening the breaker). Replaced the lb:// RestTemplate call to
 * GET /internal/activities?ids=... in Phase 23c.
 *
 * Not-found contract: GetActivities silently skips unknown ids, so an empty
 * batch means "activity does not exist" and maps to null — the same signal the
 * REST era's empty JSON array gave the chat's own-activity rule.
 *
 * Failure mode: the monolith read activities from its own database, so it had
 * no "activity store unreachable" case — its chat errors were 404 (unknown /
 * foreign activity), 503 (Claude unconfigured) and 502 (Claude failed). An
 * unreachable activity-service must NOT masquerade as the 404 (the activity
 * may exist), so the fallback maps it to a 503, the transient-dependency
 * status the frontend already handles for the unconfigured case.
 */
@Component
@Profile("!standalone")
class ActivityClient : ActivityContextClient {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Test seam (internal): in production the net.devh starter injects a
     * blocking stub on the discovery-resolved channel; tests assign a stub
     * bound to an in-process server.
     */
    @GrpcClient("activity-service")
    internal lateinit var activityStub: ActivityInternalServiceGrpc.ActivityInternalServiceBlockingStub

    @CircuitBreaker(name = "activity-service", fallbackMethod = "activityFallback")
    @Retry(name = "activity-service")
    override fun findActivity(activityId: Long): ActivityContext? =
        activityStub
            .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
            .getActivities(GetActivitiesRequest.newBuilder().addIds(activityId).build())
            .activitiesList
            .firstOrNull()
            ?.toContext()

    /**
     * Training-history context (ListRecentActivities). Unlike [findActivity],
     * failures degrade to an empty list: history enriches the coaching prompt
     * but must never take the chat down with it.
     */
    @CircuitBreaker(name = "activity-service", fallbackMethod = "historyFallback")
    @Retry(name = "activity-service")
    override fun recentActivities(userId: Long, limit: Int): List<ActivityContext> =
        activityStub
            .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
            .listRecentActivities(
                ListRecentActivitiesRequest.newBuilder().setUserId(userId).setLimit(limit).build())
            .activitiesList
            .map { it.toContext() }

    @Suppress("unused", "UNUSED_PARAMETER")
    private fun historyFallback(userId: Long, limit: Int, ex: Throwable): List<ActivityContext> {
        log.warn("ActivityClient history fallback for userId={} (cause: {})", userId, ex.message)
        return emptyList()
    }

    private fun ActivitySummaryItem.toContext() = ActivityContext(
        id = id,
        title = title,
        activityType = activityType,
        distance = if (hasDistance()) distance else null,
        duration = if (hasDuration()) duration else null,
        elevationGain = if (hasElevationGain()) elevationGain else null,
        averageHeartRate = if (hasAverageHeartRate()) averageHeartRate else null,
        calories = if (hasCalories()) calories else null,
        startTime = if (hasStartTime()) LocalDateTime.parse(startTime) else null
    )

    @Suppress("unused", "UNUSED_PARAMETER")
    private fun activityFallback(activityId: Long, ex: Throwable): ActivityContext? {
        log.warn("ActivityClient fallback for activityId={} (cause: {})", activityId, ex.message)
        throw ApiRequestException("Activity data is temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE)
    }

    /**
     * Maps the wire payload onto the slice the chat consumes. proto3 optional
     * fields answer hasX(), so absent metrics stay null and the prompt builder
     * skips them instead of printing zero-defaults; startTime arrives as the
     * ISO-8601 string LocalDateTime.toString() produced on the server.
     */
    private fun ActivityPayload.toContext() = ActivityContext(
        id = id,
        userId = userId,
        title = title,
        description = if (hasDescription()) description else null,
        activityType = activityType,
        distance = if (hasDistance()) distance else null,
        duration = if (hasDuration()) duration else null,
        elevationGain = if (hasElevationGain()) elevationGain else null,
        calories = if (hasCalories()) calories else null,
        averageSpeed = if (hasAverageSpeed()) averageSpeed else null,
        maxSpeed = if (hasMaxSpeed()) maxSpeed else null,
        averageHeartRate = if (hasAverageHeartRate()) averageHeartRate else null,
        maxHeartRate = if (hasMaxHeartRate()) maxHeartRate else null,
        averageCadence = if (hasAverageCadence()) averageCadence else null,
        averagePower = if (hasAveragePower()) averagePower else null,
        startTime = if (hasStartTime()) LocalDateTime.parse(startTime) else null
    )

    companion object {
        private const val DEADLINE_SECONDS = 3L
    }
}
