package com.strava.aiservice.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDateTime

/**
 * The slice of activity-service's cluster-internal batch payload
 * (InternalActivityResponse) the chat needs: userId for the ownership check
 * plus the metrics that ground the system prompt. Unknown fields (photos,
 * polyline, streams, ...) are ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ActivityContext(
    val id: Long? = null,
    val userId: Long? = null,
    val title: String? = null,
    val description: String? = null,
    val activityType: String? = null,
    val distance: Double? = null,
    val duration: Int? = null,
    val elevationGain: Int? = null,
    val calories: Int? = null,
    val averageSpeed: Double? = null,
    val maxSpeed: Double? = null,
    val averageHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val averageCadence: Int? = null,
    val averagePower: Int? = null,
    val startTime: LocalDateTime? = null
)

/**
 * Looks an activity up by id. ActivityClient (lb://activity-service) in prod,
 * MockActivityClient in standalone.
 */
interface ActivityContextClient {

    /**
     * @return the activity, or null when activity-service does not know the id
     *         (its internal batch endpoint silently skips missing ids).
     * @throws com.strava.aiservice.exception.ApiRequestException 503 when
     *         activity-service is unreachable.
     */
    fun findActivity(activityId: Long): ActivityContext?
}
