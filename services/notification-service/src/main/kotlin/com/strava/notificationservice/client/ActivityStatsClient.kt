package com.strava.notificationservice.client

import com.strava.grpc.activity.ActivityInternalServiceGrpc
import com.strava.grpc.activity.GetStatsRangeRequest
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import net.devh.boot.grpc.client.inject.GrpcClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/** Aggregated per-user activity totals for a date range (activity-service internal API). */
data class ActivityRangeStats(
    val totalDistanceKm: Double = 0.0,
    val totalDurationSeconds: Long = 0,
    val totalElevationGain: Double = 0.0,
    val totalCalories: Int = 0,
    val activityCount: Int = 0
)

/**
 * Calls activity-service over the cluster-internal gRPC surface
 * ({@code stride.internal.v1.ActivityInternalService.GetStatsRange}, resolved
 * from Eureka via {@code discovery:///activity-service}) with the same
 * Resilience4j retry + circuit breaker instances as the retired REST client.
 * The window rides the wire as ISO-8601 local date-times — byte-for-byte the
 * format the old query string used. Fallback returns null — the weekly summary
 * job skips that user and logs instead of failing the whole batch.
 */
@Component
class ActivityStatsClient {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Injected by net.devh from {@code grpc.client.activity-service.*}. Internal +
     * lateinit: unit tests point it at an in-process server instead of
     * Mockito-mocking generated code.
     */
    @field:GrpcClient("activity-service")
    internal lateinit var activityInternalStub: ActivityInternalServiceGrpc.ActivityInternalServiceBlockingStub

    @CircuitBreaker(name = "activity-service", fallbackMethod = "statsFallback")
    @Retry(name = "activity-service")
    fun statsForRange(userId: Long, from: LocalDateTime, to: LocalDateTime): ActivityRangeStats? {
        val stats = activityInternalStub
            .withDeadlineAfter(CALL_DEADLINE_SECONDS, TimeUnit.SECONDS)
            .getStatsRange(
                GetStatsRangeRequest.newBuilder()
                    .setUserId(userId)
                    .setFrom(ISO.format(from))
                    .setTo(ISO.format(to))
                    .build()
            )
        return ActivityRangeStats(
            totalDistanceKm = stats.totalDistanceKm,
            totalDurationSeconds = stats.totalDurationSeconds,
            totalElevationGain = stats.totalElevationGain.toDouble(),
            totalCalories = stats.totalCalories.toInt(),
            activityCount = stats.activityCount
        )
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    private fun statsFallback(userId: Long, from: LocalDateTime, to: LocalDateTime, ex: Throwable): ActivityRangeStats? {
        log.warn("ActivityStatsClient fallback for userId={} (cause: {})", userId, ex.message)
        return null
    }

    companion object {
        private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        /** Deadlines are absolute, so the ~3s budget is applied per call, never on the injected stub. */
        private const val CALL_DEADLINE_SECONDS = 3L
    }
}
