package com.strava.pipeline.client;

import com.strava.grpc.activity.ActivityInternalServiceGrpc;
import com.strava.grpc.activity.ActivityPayload;
import com.strava.grpc.activity.GetActivitiesRequest;
import com.strava.grpc.activity.GetActivitiesResponse;
import com.strava.pipeline.exception.ApiRequestException;
import io.grpc.Context;
import io.grpc.Deadline;
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
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the gRPC leg of ActivityClient (Phase 23c cutover of the
 * cluster-internal power enrichment). A fake {@code ActivityInternalService}
 * ImplBase runs on an in-process server and the client's stub seam is bound
 * to it — the generated stub is exercised for real, never Mockito-mocked.
 *
 * Resilience4j is not active here (no Spring AOP), so the raw transport
 * semantics are visible: a failed RPC surfaces as StatusRuntimeException.
 * The degradation to an empty map through the circuit-breaker fallback is
 * covered by {@link ActivityClientResilienceTest}.
 */
@DisplayName("ActivityClient gRPC Unit Tests")
class ActivityClientGrpcTest {

    private FakeActivityInternal fake;
    private Server server;
    private ManagedChannel channel;
    private ActivityClient client;

    @BeforeEach
    void setUp() throws Exception {
        fake = new FakeActivityInternal();
        String serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(fake)
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(serverName)
                .directExecutor()
                .build();
        client = new ActivityClient(new RestTemplate());
        client.setActivityInternal(ActivityInternalServiceGrpc.newBlockingStub(channel));
    }

    @AfterEach
    void tearDown() throws Exception {
        channel.shutdownNow();
        server.shutdownNow();
        channel.awaitTermination(5, TimeUnit.SECONDS);
        server.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("maps GetActivities response to power-by-id, skipping activities without power")
    void mapsPowerByIdSkippingPowerless() {
        // id 2 has no average_power set — proto3 optional distinguishes
        // "absent" from 0, so it must not appear in the map (not even as 0).
        fake.response = GetActivitiesResponse.newBuilder()
                .addActivities(ActivityPayload.newBuilder()
                        .setId(1L).setUserId(9L).setTitle("Morning ride")
                        .setActivityType("CYCLING").setAveragePower(210)
                        // streams arrive as typed repeated fields, no JSON parsing
                        .addAllElevationProfile(List.of(120.5, 122.0, 125.5))
                        .addAllPointTimestamps(List.of(1700000000L, 1700000010L, 1700000020L))
                        .build())
                .addActivities(ActivityPayload.newBuilder()
                        .setId(2L).setUserId(9L).setTitle("Powerless ride")
                        .setActivityType("CYCLING")
                        .build())
                .addActivities(ActivityPayload.newBuilder()
                        .setId(3L).setUserId(9L).setTitle("Intervals")
                        .setActivityType("CYCLING").setAveragePower(305)
                        .build())
                .build();

        Map<Long, Integer> powers = client.averagePowerByIds(List.of(1L, 2L, 3L));

        assertThat(powers).containsExactlyInAnyOrderEntriesOf(Map.of(1L, 210, 3L, 305));
        assertThat(fake.calls).isEqualTo(1);
        assertThat(fake.lastRequest.getIdsList()).containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("propagates the ~3s per-call deadline to the server")
    void appliesPerCallDeadline() {
        fake.response = GetActivitiesResponse.getDefaultInstance();

        client.averagePowerByIds(List.of(42L));

        assertThat(fake.observedDeadline).isNotNull();
        long remainingMs = fake.observedDeadline.timeRemaining(TimeUnit.MILLISECONDS);
        assertThat(remainingMs).isLessThanOrEqualTo(3000L).isPositive();
    }

    @Test
    @DisplayName("empty id list short-circuits without an RPC")
    void emptyIdsShortCircuit() {
        assertThat(client.averagePowerByIds(List.of())).isEmpty();
        assertThat(fake.calls).isZero();
    }

    @Test
    @DisplayName("server error surfaces as StatusRuntimeException (recorded, not an ignored ApiRequestException)")
    void serverErrorSurfacesAsStatusRuntimeException() {
        fake.errorStatus = Status.UNAVAILABLE.withDescription("activity-service down");

        // Not wrapped into ApiRequestException: the resilience config ignores
        // ApiRequestException only, so this exception is recorded by the
        // activity-service circuit breaker before the fallback degrades it.
        assertThatThrownBy(() -> client.averagePowerByIds(List.of(1L)))
                .isInstanceOf(StatusRuntimeException.class)
                .isNotInstanceOf(ApiRequestException.class)
                .satisfies(ex -> assertThat(((StatusRuntimeException) ex).getStatus().getCode())
                        .isEqualTo(Status.Code.UNAVAILABLE));
    }

    /**
     * Fake server-side implementation — a real ImplBase registered on the
     * in-process server, scripted per test via mutable fields.
     */
    static final class FakeActivityInternal
            extends ActivityInternalServiceGrpc.ActivityInternalServiceImplBase {

        GetActivitiesResponse response = GetActivitiesResponse.getDefaultInstance();
        Status errorStatus;
        GetActivitiesRequest lastRequest;
        Deadline observedDeadline;
        int calls;

        @Override
        public void getActivities(GetActivitiesRequest request,
                                  StreamObserver<GetActivitiesResponse> responseObserver) {
            calls++;
            lastRequest = request;
            observedDeadline = Context.current().getDeadline();
            if (errorStatus != null) {
                responseObserver.onError(errorStatus.asRuntimeException());
                return;
            }
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
