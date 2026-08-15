package com.strava.notificationservice.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Aggregated per-user activity totals for a date range (activity-service internal API). */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ActivityRangeStats(
    val totalDistanceKm: Double = 0.0,
    val totalDurationSeconds: Long = 0,
    val totalElevationGain: Double = 0.0,
    val totalCalories: Int = 0,
    val activityCount: Int = 0
)

/**
 * Calls activity-service via lb:// with Resilience4j retry + circuit
 * breaker. Fallback returns null — the weekly summary job skips that user
 * and logs instead of failing the whole batch.
 */
@Component
class ActivityStatsClient(
    private val loadBalanced: RestTemplate
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @CircuitBreaker(name = "activity-service", fallbackMethod = "statsFallback")
    @Retry(name = "activity-service")
    fun statsForRange(userId: Long, from: LocalDateTime, to: LocalDateTime): ActivityRangeStats? =
        loadBalanced.getForObject(
            "lb://activity-service/internal/activities/stats/range?userId=$userId&from=${ISO.format(from)}&to=${ISO.format(to)}",
            ActivityRangeStats::class.java
        )

    @Suppress("unused", "UNUSED_PARAMETER")
    private fun statsFallback(userId: Long, from: LocalDateTime, to: LocalDateTime, ex: Throwable): ActivityRangeStats? {
        log.warn("ActivityStatsClient fallback for userId={} (cause: {})", userId, ex.message)
        return null
    }

    companion object {
        private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }
}
