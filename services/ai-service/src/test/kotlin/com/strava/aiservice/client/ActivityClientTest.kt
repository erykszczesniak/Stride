package com.strava.aiservice.client

import com.strava.aiservice.exception.ApiRequestException
import com.strava.grpc.activity.ActivityInternalServiceGrpc
import com.strava.grpc.activity.ActivityPayload
import com.strava.grpc.activity.GetActivitiesRequest
import com.strava.grpc.activity.GetActivitiesResponse
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration
import io.grpc.Context
import io.grpc.Deadline
import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.stub.StreamObserver
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.http.HttpStatus
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

/**
 * In-process gRPC server tests (same recipe as activity-service's
 * UserVisibilityClientTest): the real blocking stub talks to a fake
 * ActivityInternalService over an in-process channel — no sockets, no Mockito
 * on generated types — assigned through the client's internal stub seam.
 * Resilience semantics (retry, breaker recording, the 503 fallback) are
 * exercised through a minimal Spring AOP context configured with the same
 * recordExceptions list as production application.yml.
 *
 * AiChatService's use of the client keeps mocking the ActivityContextClient
 * interface (see AiChatServiceTest) — the chat contract did not change.
 */
@DisplayName("ActivityClient (gRPC)")
class ActivityClientTest {

    private val fake = FakeActivityService()
    private lateinit var server: Server
    private lateinit var channel: ManagedChannel

    class FakeActivityService : ActivityInternalServiceGrpc.ActivityInternalServiceImplBase() {
        var lastRequest: GetActivitiesRequest? = null
        var lastDeadline: Deadline? = null
        var calls = 0
        var failWith: Status? = null // null -> answer with `response`
        var response: GetActivitiesResponse = GetActivitiesResponse.getDefaultInstance()

        override fun getActivities(
            request: GetActivitiesRequest,
            responseObserver: StreamObserver<GetActivitiesResponse>
        ) {
            calls++
            lastRequest = request
            lastDeadline = Context.current().deadline
            val failure = failWith
            if (failure != null) {
                responseObserver.onError(failure.asRuntimeException())
                return
            }
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        }
    }

    @BeforeEach
    fun setUp() {
        val serverName = InProcessServerBuilder.generateName()
        server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(fake)
            .build()
            .start()
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build()
    }

    @AfterEach
    fun tearDown() {
        channel.shutdownNow()
        server.shutdownNow()
    }

    /** Client wired to the in-process server through the internal stub seam. */
    private fun newClient(): ActivityClient = ActivityClient().apply {
        activityStub = ActivityInternalServiceGrpc.newBlockingStub(channel)
    }

    /**
     * Minimal Spring context that applies the production @CircuitBreaker/@Retry
     * annotations to the given client instance, with fast retry backoff.
     */
    private fun resilienceContext(target: ActivityClient): ApplicationContextRunner =
        ApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    AopAutoConfiguration::class.java,
                    CircuitBreakerAutoConfiguration::class.java,
                    RetryAutoConfiguration::class.java
                )
            )
            .withPropertyValues(
                "resilience4j.circuitbreaker.instances.activity-service.slidingWindowSize=10",
                "resilience4j.circuitbreaker.instances.activity-service.minimumNumberOfCalls=3",
                "resilience4j.circuitbreaker.instances.activity-service.recordExceptions=$RECORD_EXCEPTIONS",
                "resilience4j.retry.instances.activity-service.maxAttempts=2",
                "resilience4j.retry.instances.activity-service.waitDuration=1ms"
            )
            .withBean(ActivityClient::class.java, Supplier { target })

    @Test
    @DisplayName("asks for exactly the requested id and maps every field the prompt builder consumes")
    fun mapsAllPromptFields() {
        fake.response = GetActivitiesResponse.newBuilder()
            .addActivities(
                ActivityPayload.newBuilder()
                    .setId(100)
                    .setUserId(1)
                    .setTitle("Wednesday Group Ride")
                    .setDescription("Felt strong")
                    .setActivityType("RIDE")
                    .setDistance(69.6)
                    .setDuration(6900)
                    .setElevationGain(540)
                    .setCalories(1800)
                    .setAverageSpeed(36.3)
                    .setMaxSpeed(58.1)
                    .setAverageHeartRate(150)
                    .setMaxHeartRate(183)
                    .setAverageCadence(88)
                    .setAveragePower(213)
                    .setStartTime("2026-05-13T17:15")
                    // wire fields the chat does not consume must not disturb mapping
                    .setIsPrivate(true)
                    .setRoutePolyline("abc")
                    .addElevationProfile(1.0)
                    .addPointTimestamps(100)
                    .addPhotos("/uploads/photos/a.jpg")
                    .setCreatedAt("2026-05-13T18:00")
                    .build()
            )
            .build()

        val activity = newClient().findActivity(100)

        assertThat(fake.lastRequest!!.idsList).containsExactly(100L)
        assertThat(activity).isEqualTo(
            ActivityContext(
                id = 100,
                userId = 1,
                title = "Wednesday Group Ride",
                description = "Felt strong",
                activityType = "RIDE",
                distance = 69.6,
                duration = 6900,
                elevationGain = 540,
                calories = 1800,
                averageSpeed = 36.3,
                maxSpeed = 58.1,
                averageHeartRate = 150,
                maxHeartRate = 183,
                averageCadence = 88,
                averagePower = 213,
                startTime = LocalDateTime.of(2026, 5, 13, 17, 15)
            )
        )
    }

    @Test
    @DisplayName("absent proto3 optionals stay null so the prompt builder skips them (never zero-defaults)")
    fun absentOptionalsStayNull() {
        fake.response = GetActivitiesResponse.newBuilder()
            .addActivities(
                ActivityPayload.newBuilder()
                    .setId(2)
                    .setUserId(5)
                    .setTitle("Bare swim")
                    .setActivityType("SWIM")
                    .build()
            )
            .build()

        val activity = newClient().findActivity(2)!!

        assertThat(activity.id).isEqualTo(2)
        assertThat(activity.userId).isEqualTo(5)
        assertThat(activity.title).isEqualTo("Bare swim")
        assertThat(activity.activityType).isEqualTo("SWIM")
        assertThat(activity.description).isNull()
        assertThat(activity.distance).isNull()
        assertThat(activity.duration).isNull()
        assertThat(activity.elevationGain).isNull()
        assertThat(activity.calories).isNull()
        assertThat(activity.averageSpeed).isNull()
        assertThat(activity.maxSpeed).isNull()
        assertThat(activity.averageHeartRate).isNull()
        assertThat(activity.maxHeartRate).isNull()
        assertThat(activity.averageCadence).isNull()
        assertThat(activity.averagePower).isNull()
        assertThat(activity.startTime).isNull()
    }

    @Test
    @DisplayName("an empty batch (unknown id silently skipped) maps to null — the chat's not-found signal")
    fun emptyBatchMeansNotFound() {
        assertThat(newClient().findActivity(999)).isNull()
        assertThat(fake.lastRequest!!.idsList).containsExactly(999L)
    }

    @Test
    @DisplayName("every call carries the ~3s deadline")
    fun appliesDeadline() {
        newClient().findActivity(1)

        assertThat(fake.lastDeadline).isNotNull
        assertThat(fake.lastDeadline!!.timeRemaining(TimeUnit.MILLISECONDS))
            .isGreaterThan(0)
            .isLessThanOrEqualTo(3000)
    }

    @Test
    @DisplayName("without the resilience proxy a server failure surfaces as StatusRuntimeException")
    fun rawStubPropagatesStatusRuntimeException() {
        fake.failWith = Status.UNAVAILABLE

        assertThatThrownBy { newClient().findActivity(1) }
            .isInstanceOf(StatusRuntimeException::class.java)
    }

    @Test
    @DisplayName("unreachable activity-service -> 503 'Activity data is temporarily unavailable'; retried and recorded by the breaker")
    fun unreachableServiceBecomes503() {
        fake.failWith = Status.UNAVAILABLE

        resilienceContext(newClient()).run { context ->
            val client = context.getBean(ActivityClient::class.java)

            assertThatThrownBy { client.findActivity(1) }
                .isInstanceOf(ApiRequestException::class.java)
                .hasMessage("Activity data is temporarily unavailable")
                .extracting { (it as ApiRequestException).status }
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)

            // @Retry attempted the call maxAttempts times before giving up
            assertThat(fake.calls).isEqualTo(2)
            // StatusRuntimeException is on the recordExceptions list -> the
            // breaker counted the failure (it would never open otherwise)
            val registry = context.getBean(CircuitBreakerRegistry::class.java)
            assertThat(registry.circuitBreaker("activity-service").metrics.numberOfFailedCalls)
                .isGreaterThanOrEqualTo(1)
        }
    }

    @Test
    @DisplayName("a deadline overrun degrades to the same 503, never a hung request")
    fun deadlineOverrunBecomes503() {
        fake.failWith = Status.DEADLINE_EXCEEDED

        resilienceContext(newClient()).run { context ->
            val client = context.getBean(ActivityClient::class.java)

            assertThatThrownBy { client.findActivity(1) }
                .isInstanceOf(ApiRequestException::class.java)
                .extracting { (it as ApiRequestException).status }
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        }
    }

    @Test
    @DisplayName("through the resilience proxy a missing activity is still null, not a failure")
    fun missingActivityIsNullThroughProxy() {
        resilienceContext(newClient()).run { context ->
            val client = context.getBean(ActivityClient::class.java)

            assertThat(client.findActivity(999)).isNull()

            val registry = context.getBean(CircuitBreakerRegistry::class.java)
            assertThat(registry.circuitBreaker("activity-service").metrics.numberOfFailedCalls).isZero()
        }
    }

    companion object {
        /** Mirrors the resilience4j config in src/main/resources/application.yml. */
        private const val RECORD_EXCEPTIONS = "io.grpc.StatusRuntimeException," +
            "java.io.IOException," +
            "java.util.concurrent.TimeoutException"
    }
}
