package com.strava.aiservice

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles

/**
 * Boots the real standalone wiring (H2 + Flyway + mock AI / activity clients)
 * on a random port and walks the public contract end to end — the same checks
 * the manual smoke-test recipe performs against the running jar.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("standalone")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@DisplayName("Standalone smoke test")
class StandaloneSmokeTest {

    @Autowired
    lateinit var rest: TestRestTemplate

    private fun headers(userId: Long = 1): HttpHeaders = HttpHeaders().apply {
        set("X-User-Id", userId.toString())
        set("X-User-Email", "athlete@example.com")
        set("X-User-Roles", "USER")
        contentType = MediaType.APPLICATION_JSON
    }

    @Test
    @Order(1)
    @DisplayName("actuator health is up without any external dependency")
    fun healthIsUp() {
        val response = rest.getForEntity("/actuator/health", String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).contains("UP")
    }

    @Test
    @Order(2)
    @DisplayName("requests without gateway identity headers are rejected")
    fun rejectsAnonymous() {
        val response = rest.exchange(
            "/api/v1/ai/activities/1/chat", HttpMethod.GET, HttpEntity<Void>(HttpHeaders()), String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    @Order(3)
    @DisplayName("generate-goal returns the raw JSON string from the model")
    fun generateGoal() {
        val response = rest.exchange(
            "/api/v1/ai/generate-goal", HttpMethod.POST,
            HttpEntity("""{"goalType": "distance", "timeframe": "month", "description": "run more"}""", headers()),
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).contains("smartGoal").contains("milestones").contains("tips")
    }

    @Test
    @Order(4)
    @DisplayName("generate-plan returns the raw JSON string from the model")
    fun generatePlan() {
        val response = rest.exchange(
            "/api/v1/ai/generate-plan", HttpMethod.POST,
            HttpEntity("{}", headers()),
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).contains("weeks").contains("workouts")
    }

    @Test
    @Order(5)
    @DisplayName("chat lifecycle: empty GET, POST persists both turns, second POST appends, DELETE clears")
    fun chatLifecycle() {
        // Empty-conversation shape before any message.
        val empty = rest.exchange(
            "/api/v1/ai/activities/42/chat", HttpMethod.GET, HttpEntity<Void>(headers()), String::class.java
        )
        assertThat(empty.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(empty.body).contains("\"activityId\":42").contains("\"messages\":[]")

        // First message persists USER + ASSISTANT turns in one transaction.
        val first = rest.exchange(
            "/api/v1/ai/activities/42/chat", HttpMethod.POST,
            HttpEntity("""{"message": "Analyze this activity."}""", headers()),
            String::class.java
        )
        assertThat(first.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(first.body).contains("\"USER\"").contains("\"ASSISTANT\"").contains("Analyze this activity.")

        // Second message appends to the same conversation (4 turns total).
        val second = rest.exchange(
            "/api/v1/ai/activities/42/chat", HttpMethod.POST,
            HttpEntity("""{"message": "How was my pacing?"}""", headers()),
            String::class.java
        )
        assertThat(second.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(second.body!!.split("\"role\"").size - 1).isEqualTo(4)

        // Blank message is a 400 with the branch-wide message property.
        val blank = rest.exchange(
            "/api/v1/ai/activities/42/chat", HttpMethod.POST,
            HttpEntity("""{"message": "  "}""", headers()),
            String::class.java
        )
        assertThat(blank.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(blank.body).contains("\"message\"")

        // DELETE clears; the follow-up GET is the empty shape again.
        val delete = rest.exchange(
            "/api/v1/ai/activities/42/chat", HttpMethod.DELETE, HttpEntity<Void>(headers()), String::class.java
        )
        assertThat(delete.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

        val afterDelete = rest.exchange(
            "/api/v1/ai/activities/42/chat", HttpMethod.GET, HttpEntity<Void>(headers()), String::class.java
        )
        assertThat(afterDelete.body).contains("\"messages\":[]")
    }

    @Test
    @Order(6)
    @DisplayName("a foreign user's chat probe is answered with the monolith's 404")
    fun foreignActivityIs404() {
        // The standalone mock activity is owned by user 1 — user 2 must get not-found.
        val response = rest.exchange(
            "/api/v1/ai/activities/42/chat", HttpMethod.GET, HttpEntity<Void>(headers(userId = 2)), String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(response.body).contains("Activity not found")
    }
}
