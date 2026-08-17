package com.strava.challengegoal.client;

import com.strava.grpc.activity.ActivityInternalServiceGrpc;
import com.strava.grpc.activity.GetUserActivitySummaryRequest;
import com.strava.grpc.activity.UserActivitySummary;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the gRPC cutover of the activity summary lookup: one
 * {@code GetUserActivitySummary} call carrying (userId, optional sport,
 * ISO local-date-time window) instead of the old REST page-and-sum — the
 * aggregation semantics themselves now live (and are tested) server-side
 * in activity-service. Generated stubs are final, so the test runs against
 * a fake ImplBase behind an in-process server rather than a Mockito mock.
 */
@DisplayName("ActivityClient Unit Tests")
class ActivityClientTest {

    private Server server;
    private ManagedChannel channel;
    private ActivityClient activityClient;

    private final AtomicReference<GetUserActivitySummaryRequest> lastRequest = new AtomicReference<>();
    private UserActivitySummary cannedResponse = UserActivitySummary.getDefaultInstance();
    private Status cannedError;

    private final ActivityInternalServiceGrpc.ActivityInternalServiceImplBase fakeService =
            new ActivityInternalServiceGrpc.ActivityInternalServiceImplBase() {
                @Override
                public void getUserActivitySummary(GetUserActivitySummaryRequest request,
                                                   StreamObserver<UserActivitySummary> responseObserver) {
                    lastRequest.set(request);
                    if (cannedError != null) {
                        responseObserver.onError(cannedError.asRuntimeException());
                        return;
                    }
                    responseObserver.onNext(cannedResponse);
                    responseObserver.onCompleted();
                }
            };

    @BeforeEach
    void setUp() throws IOException {
        String serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(fakeService)
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        activityClient = new ActivityClient(ActivityInternalServiceGrpc.newBlockingStub(channel));
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    @DisplayName("Should send one summary request with sport filter and ISO window, mapping the totals")
    void shouldSendSummaryRequestAndMapTotals() {
        LocalDateTime from = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 8, 31, 23, 59);
        cannedResponse = UserActivitySummary.newBuilder()
                .setTotalDistanceKm(15.0)
                .setTotalDurationSeconds(5400)
                .setTotalElevationMeters(150.0)
                .setActivityCount(2)
                .build();

        ActivitySummary summary = activityClient.summaryFor(7L, "RUN", from, to);

        GetUserActivitySummaryRequest sent = lastRequest.get();
        assertThat(sent.getUserId()).isEqualTo(7L);
        assertThat(sent.hasSportFilter()).isTrue();
        assertThat(sent.getSportFilter()).isEqualTo("RUN");
        assertThat(sent.getFrom()).isEqualTo("2026-08-01T00:00");
        assertThat(sent.getTo()).isEqualTo("2026-08-31T23:59");

        assertThat(summary.totalDistanceKm()).isEqualTo(15.0);
        assertThat(summary.totalDurationSeconds()).isEqualTo(5400);
        assertThat(summary.totalElevationMeters()).isEqualTo(150.0);
        assertThat(summary.activityCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should leave the sport filter unset when no sport filter is given")
    void shouldLeaveSportFilterUnsetWhenNoFilter() {
        cannedResponse = UserActivitySummary.newBuilder()
                .setTotalDistanceKm(50.0)
                .setActivityCount(2)
                .build();

        ActivitySummary summary = activityClient.summaryFor(7L, null,
                LocalDateTime.of(2026, 8, 1, 0, 0), LocalDateTime.of(2026, 8, 31, 23, 59));

        assertThat(lastRequest.get().hasSportFilter()).isFalse();
        assertThat(summary.totalDistanceKm()).isEqualTo(50.0);
        assertThat(summary.activityCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should map an all-defaults response to EMPTY")
    void shouldMapDefaultsResponseToEmpty() {
        cannedResponse = UserActivitySummary.getDefaultInstance();

        ActivitySummary summary = activityClient.summaryFor(7L, "RUN",
                LocalDateTime.now().minusDays(7), LocalDateTime.now());

        assertThat(summary).isEqualTo(ActivitySummary.EMPTY);
    }

    @Test
    @DisplayName("Should surface StatusRuntimeException so the circuit breaker records it")
    void shouldSurfaceStatusRuntimeException() {
        // The Resilience4j proxy (recordExceptions: io.grpc.StatusRuntimeException)
        // turns this into the ActivitySummary.EMPTY fallback in the Spring context;
        // the raw client must let it propagate.
        cannedError = Status.UNAVAILABLE.withDescription("activity-service down");

        assertThatThrownBy(() -> activityClient.summaryFor(7L, "RUN",
                LocalDateTime.of(2026, 8, 1, 0, 0), LocalDateTime.of(2026, 8, 31, 23, 59)))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessageContaining("UNAVAILABLE");
    }
}
