package com.strava.aiservice.client

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Standalone-profile stand-in for ActivityClient: there is no activity-service
 * (or Eureka) to call, so every id resolves to a canned run owned by user 1 —
 * smoke-test the chat with the gateway-less header `X-User-Id: 1`. Any other
 * user id exercises the foreign-activity 404 path.
 */
@Component
@Profile("standalone")
class MockActivityClient : ActivityContextClient {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun findActivity(activityId: Long): ActivityContext? {
        log.info("[mock] findActivity({}) — returning canned activity owned by user 1", activityId)
        return ActivityContext(
            id = activityId,
            userId = 1,
            title = "Morning Run",
            activityType = "RUN",
            distance = 10.0,
            duration = 3000,
            elevationGain = 85,
            averageHeartRate = 152,
            startTime = LocalDateTime.now().minusDays(1)
        )
    }
}
