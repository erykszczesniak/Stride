package com.strava.userservice.client;

import com.strava.grpc.activity.ActivityInternalServiceGrpc;
import com.strava.grpc.activity.ActivityStatsRange;
import com.strava.grpc.activity.GetStatsRangeRequest;
import com.strava.userservice.user.ActivityStatsPort.ActivityTotals;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Totals go through a real in-process gRPC server backed by a programmable
 * fake ImplBase (generated stubs are never Mockito-mocked); the stub is handed
 * to the adapter through its package-private test seam. Recent activities stay
 * on the mocked RestTemplate — they never left the public REST endpoint.
 */
@ExtendWith(MockitoExtension.class)
class ActivityStatsPortAdapterTest {

    @Mock
    private RestTemplate restTemplate;

    private final FakeActivityInternalService fakeActivityService = new FakeActivityInternalService();

    private Server grpcServer;
    private ManagedChannel channel;

    private ActivityStatsPortAdapter port;

    @BeforeEach
    void setUp() throws IOException {
        String serverName = InProcessServerBuilder.generateName();
        grpcServer = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(fakeActivityService)
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();

        port = new ActivityStatsPortAdapter(restTemplate);
        port.activityInternalStub = ActivityInternalServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
        grpcServer.shutdownNow();
    }

    /** Programmable fake for the generated service — records the last request. */
    private static final class FakeActivityInternalService
            extends ActivityInternalServiceGrpc.ActivityInternalServiceImplBase {

        private GetStatsRangeRequest lastStatsRequest;
        private ActivityStatsRange statsResponse = ActivityStatsRange.getDefaultInstance();
        private Status statsError;

        @Override
        public void getStatsRange(GetStatsRangeRequest request,
                                  StreamObserver<ActivityStatsRange> responseObserver) {
            lastStatsRequest = request;
            if (statsError != null) {
                responseObserver.onError(statsError.asRuntimeException());
                return;
            }
            responseObserver.onNext(statsResponse);
            responseObserver.onCompleted();
        }
    }

    @Test
    void totalsMapStatsRangeResponse() {
        fakeActivityService.statsResponse = ActivityStatsRange.newBuilder()
                .setActivityCount(7)
                .setTotalDistanceKm(123.5)
                .setTotalDurationSeconds(3600L)
                .setTotalElevationGain(250L)
                .setTotalCalories(4200L)
                .build();

        ActivityTotals totals = port.totalsFor(9L);

        assertThat(totals.totalActivities()).isEqualTo(7);
        assertThat(totals.totalDistance()).isEqualTo(123.5);
        assertThat(totals.totalDurationSeconds()).isEqualTo(3600L);
        assertThat(totals.totalElevationMeters()).isEqualTo(250L);
        assertThat(totals.totalCalories()).isEqualTo(4200L);
    }

    @Test
    void totalsRequestTheAllTimeWindowTheRestCallUsed() {
        port.totalsFor(9L);

        GetStatsRangeRequest sent = fakeActivityService.lastStatsRequest;
        assertThat(sent).isNotNull();
        assertThat(sent.getUserId()).isEqualTo(9L);
        assertThat(sent.getFrom()).isEqualTo("1970-01-01T00:00:00");
        assertThat(sent.getTo()).isEqualTo("2999-01-01T00:00:00");
    }

    @Test
    void totalsWithDefaultResponseDegradeToEmpty() {
        fakeActivityService.statsResponse = ActivityStatsRange.getDefaultInstance();

        assertThat(port.totalsFor(9L)).isEqualTo(ActivityTotals.empty());
    }

    /**
     * Server errors must surface as StatusRuntimeException from the raw call:
     * the Resilience4j annotations (default record-everything config) turn them
     * into retries / circuit-breaker signals and the zero-totals fallback.
     */
    @Test
    void totalsSurfaceStatusRuntimeExceptionForResilience() {
        fakeActivityService.statsError = Status.UNAVAILABLE.withDescription("activity-service down");

        assertThatThrownBy(() -> port.totalsFor(9L))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(e -> assertThat(((StatusRuntimeException) e).getStatus().getCode())
                        .isEqualTo(Status.Code.UNAVAILABLE));
    }

    @Test
    void recentActivitiesForwardViewerIdHeader() {
        when(restTemplate.exchange(contains("/api/v1/activities/user/{userId}"), eq(HttpMethod.GET),
                any(HttpEntity.class), any(ParameterizedTypeReference.class), eq(9L)))
                .thenAnswer(inv -> {
                    HttpEntity<?> entity = inv.getArgument(2);
                    assertThat(entity.getHeaders().getFirst("X-User-Id")).isEqualTo("4");
                    return ResponseEntity.ok(List.of(Map.of("id", 1)));
                });

        List<Object> recent = port.recentPublicActivities(9L, 4L);

        assertThat(recent).hasSize(1);
    }

    @Test
    void recentActivitiesOmitHeaderForAnonymousViewer() {
        when(restTemplate.exchange(contains("/api/v1/activities/user/{userId}"), eq(HttpMethod.GET),
                any(HttpEntity.class), any(ParameterizedTypeReference.class), eq(9L)))
                .thenAnswer(inv -> {
                    HttpEntity<?> entity = inv.getArgument(2);
                    assertThat(entity.getHeaders().containsKey("X-User-Id")).isFalse();
                    return ResponseEntity.ok(List.of());
                });

        assertThat(port.recentPublicActivities(9L, null)).isEmpty();
    }
}
