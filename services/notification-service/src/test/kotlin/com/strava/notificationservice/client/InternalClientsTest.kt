package com.strava.notificationservice.client

import com.strava.grpc.activity.ActivityInternalServiceGrpc
import com.strava.grpc.activity.ActivityStatsRange
import com.strava.grpc.activity.GetStatsRangeRequest
import com.strava.grpc.user.GetNotificationProfileRequest
import com.strava.grpc.user.ListWeeklySummaryRecipientsRequest
import com.strava.grpc.user.ListWeeklySummaryRecipientsResponse
import com.strava.grpc.user.NotificationProfile
import com.strava.grpc.user.UserInternalServiceGrpc
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
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * Wire contract for the cluster-internal gRPC calls, exercised against a real
 * in-process server backed by programmable fake ImplBase services (generated
 * stubs are never Mockito-mocked); the stubs are handed to the clients through
 * their internal test seam. The Resilience4j fallback wiring (annotations) is
 * exercised at runtime via Spring AOP and covered by the service-level
 * "client returned null" tests (skip email / skip user / abort job).
 */
class InternalClientsTest {

    private val fakeUserService = FakeUserInternalService()
    private val fakeActivityService = FakeActivityInternalService()

    private lateinit var grpcServer: Server
    private lateinit var channel: ManagedChannel

    private lateinit var userClient: UserProfileClient
    private lateinit var activityClient: ActivityStatsClient

    @BeforeEach
    fun setUp() {
        val serverName = InProcessServerBuilder.generateName()
        grpcServer = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(fakeUserService)
            .addService(fakeActivityService)
            .build()
            .start()
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build()

        userClient = UserProfileClient().apply {
            userInternalStub = UserInternalServiceGrpc.newBlockingStub(channel)
        }
        activityClient = ActivityStatsClient().apply {
            activityInternalStub = ActivityInternalServiceGrpc.newBlockingStub(channel)
        }
    }

    @AfterEach
    fun tearDown() {
        channel.shutdownNow()
        grpcServer.shutdownNow()
    }

    // ---------------------------------------------------------------- profile

    @Test
    fun `notification profile is fetched over gRPC and mapped onto the REST-era DTO`() {
        fakeUserService.profileResponse = NotificationProfile.newBuilder()
            .setId(7)
            .setEmail("u7@example.com")
            .setFirstName("Alice")
            .setFullName("Alice Doe")
            .setEmailNotifications(true)
            .setPushNotifications(false)
            .setWeeklySummary(true)
            .setActive(true)
            .build() // lastName + avatar deliberately unset

        val profile = userClient.notificationProfile(7)

        assertThat(profile).isEqualTo(
            UserNotificationProfile(
                id = 7,
                email = "u7@example.com",
                firstName = "Alice",
                lastName = null,
                fullName = "Alice Doe",
                avatar = null,
                emailNotifications = true,
                pushNotifications = false,
                weeklySummary = true,
                active = true
            )
        )
        assertThat(fakeUserService.lastProfileRequest?.userId).isEqualTo(7L)
    }

    @Test
    fun `unknown user maps NOT_FOUND to null like the old REST 404`() {
        fakeUserService.profileError = Status.NOT_FOUND.withDescription("User not found")

        assertThat(userClient.notificationProfile(99)).isNull()
    }

    @Test
    fun `profile call surfaces non-NOT_FOUND errors for the resilience fallback`() {
        fakeUserService.profileError = Status.UNAVAILABLE.withDescription("user-service down")

        assertThatThrownBy { userClient.notificationProfile(7) }
            .isInstanceOfSatisfying(StatusRuntimeException::class.java) {
                assertThat(it.status.code).isEqualTo(Status.Code.UNAVAILABLE)
            }
    }

    // ------------------------------------------------------------- recipients

    @Test
    fun `weekly summary recipients keep the page abstraction the job consumes`() {
        fakeUserService.recipientsResponse = ListWeeklySummaryRecipientsResponse.newBuilder()
            .addRecipients(recipient(1))
            .addRecipients(recipient(2))
            .setTotalElements(3)
            .setTotalPages(2)
            .build()

        val page = userClient.weeklySummaryRecipients(0, 2)

        assertThat(page).isNotNull
        assertThat(page!!.content.map { it.id }).containsExactly(1L, 2L)
        assertThat(page.content.map { it.email }).containsExactly("u1@example.com", "u2@example.com")
        assertThat(page.last).isFalse()
        assertThat(fakeUserService.lastRecipientsRequest?.page).isEqualTo(0)
        assertThat(fakeUserService.lastRecipientsRequest?.size).isEqualTo(2)
    }

    @Test
    fun `final recipients page is marked last`() {
        fakeUserService.recipientsResponse = ListWeeklySummaryRecipientsResponse.newBuilder()
            .addRecipients(recipient(3))
            .setTotalElements(3)
            .setTotalPages(2)
            .build()

        assertThat(userClient.weeklySummaryRecipients(1, 2)?.last).isTrue()
    }

    @Test
    fun `empty recipients result is last so the job terminates`() {
        // default instance: no recipients, totalPages == 0
        val page = userClient.weeklySummaryRecipients(0, 2)

        assertThat(page?.content).isEmpty()
        assertThat(page?.last).isTrue()
    }

    @Test
    fun `recipients call surfaces errors for the resilience fallback`() {
        fakeUserService.recipientsError = Status.DEADLINE_EXCEEDED.withDescription("too slow")

        assertThatThrownBy { userClient.weeklySummaryRecipients(0, 2) }
            .isInstanceOf(StatusRuntimeException::class.java)
    }

    // ------------------------------------------------------------ range stats

    @Test
    fun `range stats are fetched over gRPC with the ISO window the REST call used`() {
        fakeActivityService.statsResponse = ActivityStatsRange.newBuilder()
            .setTotalDistanceKm(12.5)
            .setTotalDurationSeconds(3600)
            .setTotalElevationGain(100)
            .setTotalCalories(500)
            .setActivityCount(2)
            .build()

        val stats = activityClient.statsForRange(
            7,
            LocalDateTime.of(2026, 8, 3, 0, 0, 0),
            LocalDateTime.of(2026, 8, 9, 23, 59, 59)
        )

        assertThat(stats).isEqualTo(ActivityRangeStats(12.5, 3600, 100.0, 500, 2))
        val sent = fakeActivityService.lastStatsRequest
        assertThat(sent).isNotNull
        assertThat(sent!!.userId).isEqualTo(7L)
        assertThat(sent.from).isEqualTo("2026-08-03T00:00:00")
        assertThat(sent.to).isEqualTo("2026-08-09T23:59:59")
    }

    @Test
    fun `stats call surfaces errors for the resilience fallback`() {
        fakeActivityService.statsError = Status.UNAVAILABLE.withDescription("activity-service down")

        assertThatThrownBy {
            activityClient.statsForRange(7, LocalDateTime.now().minusWeeks(1), LocalDateTime.now())
        }.isInstanceOfSatisfying(StatusRuntimeException::class.java) {
            assertThat(it.status.code).isEqualTo(Status.Code.UNAVAILABLE)
        }
    }

    // ----------------------------------------------------------------- helpers

    private fun recipient(id: Long): NotificationProfile = NotificationProfile.newBuilder()
        .setId(id)
        .setEmail("u$id@example.com")
        .setWeeklySummary(true)
        .setActive(true)
        .build()

    /** Programmable fake for the generated user service — records the last request. */
    private class FakeUserInternalService : UserInternalServiceGrpc.UserInternalServiceImplBase() {
        var lastProfileRequest: GetNotificationProfileRequest? = null
        var profileResponse: NotificationProfile = NotificationProfile.getDefaultInstance()
        var profileError: Status? = null

        var lastRecipientsRequest: ListWeeklySummaryRecipientsRequest? = null
        var recipientsResponse: ListWeeklySummaryRecipientsResponse =
            ListWeeklySummaryRecipientsResponse.getDefaultInstance()
        var recipientsError: Status? = null

        override fun getNotificationProfile(
            request: GetNotificationProfileRequest,
            responseObserver: StreamObserver<NotificationProfile>
        ) {
            lastProfileRequest = request
            val error = profileError
            if (error != null) {
                responseObserver.onError(error.asRuntimeException())
                return
            }
            responseObserver.onNext(profileResponse)
            responseObserver.onCompleted()
        }

        override fun listWeeklySummaryRecipients(
            request: ListWeeklySummaryRecipientsRequest,
            responseObserver: StreamObserver<ListWeeklySummaryRecipientsResponse>
        ) {
            lastRecipientsRequest = request
            val error = recipientsError
            if (error != null) {
                responseObserver.onError(error.asRuntimeException())
                return
            }
            responseObserver.onNext(recipientsResponse)
            responseObserver.onCompleted()
        }
    }

    /** Programmable fake for the generated activity service — records the last request. */
    private class FakeActivityInternalService : ActivityInternalServiceGrpc.ActivityInternalServiceImplBase() {
        var lastStatsRequest: GetStatsRangeRequest? = null
        var statsResponse: ActivityStatsRange = ActivityStatsRange.getDefaultInstance()
        var statsError: Status? = null

        override fun getStatsRange(
            request: GetStatsRangeRequest,
            responseObserver: StreamObserver<ActivityStatsRange>
        ) {
            lastStatsRequest = request
            val error = statsError
            if (error != null) {
                responseObserver.onError(error.asRuntimeException())
                return
            }
            responseObserver.onNext(statsResponse)
            responseObserver.onCompleted()
        }
    }
}
