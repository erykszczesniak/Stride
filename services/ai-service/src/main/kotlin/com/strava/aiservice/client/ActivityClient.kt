package com.strava.aiservice.client

import com.strava.aiservice.exception.ApiRequestException
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.slf4j.LoggerFactory
import org.springframework.cloud.client.loadbalancer.LoadBalanced
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

/**
 * Fetches the activity backing a chat from activity-service's cluster-internal
 * batch API (GET /internal/activities?ids=...) via lb:// + Eureka, wrapped in
 * Resilience4j retry + circuit breaker.
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
class ActivityClient(
    @LoadBalanced private val loadBalanced: RestTemplate
) : ActivityContextClient {

    private val log = LoggerFactory.getLogger(javaClass)

    @CircuitBreaker(name = "activity-service", fallbackMethod = "activityFallback")
    @Retry(name = "activity-service")
    override fun findActivity(activityId: Long): ActivityContext? =
        loadBalanced
            .getForObject("lb://activity-service/internal/activities?ids=$activityId", Array<ActivityContext>::class.java)
            ?.firstOrNull()

    @Suppress("unused", "UNUSED_PARAMETER")
    private fun activityFallback(activityId: Long, ex: Throwable): ActivityContext? {
        log.warn("ActivityClient fallback for activityId={} (cause: {})", activityId, ex.message)
        throw ApiRequestException("Activity data is temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE)
    }
}
