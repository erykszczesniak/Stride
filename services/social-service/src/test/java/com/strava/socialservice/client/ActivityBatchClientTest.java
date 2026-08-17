package com.strava.socialservice.client;

import com.strava.grpc.activity.ActivityInternalServiceGrpc;
import com.strava.grpc.activity.ActivityPayload;
import com.strava.grpc.activity.GetActivitiesRequest;
import com.strava.grpc.activity.GetActivitiesResponse;
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

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the gRPC client against a fake ActivityInternalService bound over
 * the in-process transport — a real stub, a real call, no Mockito on generated
 * classes. Resilience4j annotations are AOP-driven and not active here; the
 * degraded-snapshot fallback semantics stay covered by FeedQueryServiceTest,
 * while this class asserts what the resilience layer sees: a successful map
 * (missing ids absent) or a thrown StatusRuntimeException.
 */
@DisplayName("ActivityBatchClient")
class ActivityBatchClientTest {

    private Server server;
    private ManagedChannel channel;
    private FakeActivityInternalService fake;
    private ActivityBatchClient client;

    @BeforeEach
    void setUp() throws IOException {
        fake = new FakeActivityInternalService();
        String name = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(name)
                .directExecutor()
                .addService(fake)
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        client = new ActivityBatchClient();
        client.activityStub = ActivityInternalServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        channel.shutdownNow();
        server.shutdownNow();
        channel.awaitTermination(5, TimeUnit.SECONDS);
        server.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("maps a fully-populated payload onto the DTO with REST field parity")
    void mapsFullPayload() {
        fake.store.put(6L, ActivityPayload.newBuilder()
                .setId(6L).setUserId(1L).setTitle("Park Run").setActivityType("RUN").setIsPrivate(true)
                .setDescription("desc")
                .setDistance(8.5).setDuration(2880).setElevationGain(156).setCalories(620)
                .setAverageSpeed(10.6).setMaxSpeed(14.2)
                .setAverageHeartRate(150).setMaxHeartRate(180)
                .setAverageCadence(86).setAveragePower(240)
                .setStartTime("2026-07-01T08:00:00").setEndTime("2026-07-01T08:48:00")
                .setStartLocation("Warsaw").setEndLocation("Warsaw Park")
                .setRoutePolyline("poly{}")
                .addElevationProfile(100.5).addElevationProfile(101.25)
                .addPointTimestamps(0).addPointTimestamps(60)
                .setMapImageUrl("map.png")
                .addPhotos("p1.jpg").addPhotos("p2.jpg")
                .setCreatedAt("2026-07-01T09:00:00").setUpdatedAt("2026-07-02T09:30:00")
                .build());

        Map<Long, InternalActivityDto> result = client.activitiesById(List.of(6L)).orElseThrow();

        InternalActivityDto dto = result.get(6L);
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(6L);
        assertThat(dto.userId()).isEqualTo(1L);
        assertThat(dto.title()).isEqualTo("Park Run");
        assertThat(dto.description()).isEqualTo("desc");
        assertThat(dto.activityType()).isEqualTo("RUN");
        assertThat(dto.distance()).isEqualTo(8.5);
        assertThat(dto.duration()).isEqualTo(2880);
        assertThat(dto.elevationGain()).isEqualTo(156);
        assertThat(dto.calories()).isEqualTo(620);
        assertThat(dto.averageSpeed()).isEqualTo(10.6);
        assertThat(dto.maxSpeed()).isEqualTo(14.2);
        assertThat(dto.averageHeartRate()).isEqualTo(150);
        assertThat(dto.maxHeartRate()).isEqualTo(180);
        assertThat(dto.averageCadence()).isEqualTo(86);
        assertThat(dto.averagePower()).isEqualTo(240);
        assertThat(dto.startTime()).isEqualTo(LocalDateTime.of(2026, 7, 1, 8, 0));
        assertThat(dto.endTime()).isEqualTo(LocalDateTime.of(2026, 7, 1, 8, 48));
        assertThat(dto.startLocation()).isEqualTo("Warsaw");
        assertThat(dto.endLocation()).isEqualTo("Warsaw Park");
        assertThat(dto.routePolyline()).isEqualTo("poly{}");
        assertThat(dto.elevationProfile()).containsExactly(100.5, 101.25);
        assertThat(dto.pointTimestamps()).containsExactly(0L, 60L);
        assertThat(dto.mapImageUrl()).isEqualTo("map.png");
        assertThat(dto.isPrivate()).isTrue();
        assertThat(dto.photos()).containsExactly("p1.jpg", "p2.jpg");
        assertThat(dto.createdAt()).isEqualTo(LocalDateTime.of(2026, 7, 1, 9, 0));
        assertThat(dto.updatedAt()).isEqualTo(LocalDateTime.of(2026, 7, 2, 9, 30));
    }

    @Test
    @DisplayName("unset proto3 optionals become null, empty streams become null arrays, photos stay an empty list")
    void mapsAbsentFieldsToNull() {
        fake.store.put(7L, ActivityPayload.newBuilder()
                .setId(7L).setUserId(2L).setTitle("Bare Minimum").setActivityType("WORKOUT").setIsPrivate(false)
                .build());

        InternalActivityDto dto = client.activitiesById(List.of(7L)).orElseThrow().get(7L);

        assertThat(dto).isNotNull();
        assertThat(dto.description()).isNull();
        assertThat(dto.distance()).isNull();
        assertThat(dto.duration()).isNull();
        assertThat(dto.elevationGain()).isNull();
        assertThat(dto.calories()).isNull();
        assertThat(dto.averageSpeed()).isNull();
        assertThat(dto.maxSpeed()).isNull();
        assertThat(dto.averageHeartRate()).isNull();
        assertThat(dto.maxHeartRate()).isNull();
        assertThat(dto.averageCadence()).isNull();
        assertThat(dto.averagePower()).isNull();
        assertThat(dto.startTime()).isNull();
        assertThat(dto.endTime()).isNull();
        assertThat(dto.startLocation()).isNull();
        assertThat(dto.endLocation()).isNull();
        assertThat(dto.routePolyline()).isNull();
        // no stream stored upstream → REST sent null arrays, so empty repeated maps back to null
        assertThat(dto.elevationProfile()).isNull();
        assertThat(dto.pointTimestamps()).isNull();
        assertThat(dto.mapImageUrl()).isNull();
        assertThat(dto.isPrivate()).isFalse();
        // the REST producer never sent null photos — empty list means "no photos"
        assertThat(dto.photos()).isEmpty();
        assertThat(dto.createdAt()).isNull();
        assertThat(dto.updatedAt()).isNull();
    }

    @Test
    @DisplayName("ids the server silently skips are absent from the map so the caller drops those rows")
    void missingIdsAbsentFromMap() {
        fake.store.put(6L, ActivityPayload.newBuilder()
                .setId(6L).setUserId(1L).setTitle("Park Run").setActivityType("RUN").setIsPrivate(false)
                .build());

        Optional<Map<Long, InternalActivityDto>> result = client.activitiesById(List.of(6L, 99L));

        assertThat(result).isPresent();
        assertThat(result.orElseThrow()).containsOnlyKeys(6L);
        assertThat(fake.lastRequestedIds).containsExactly(6L, 99L);
    }

    @Test
    @DisplayName("null or empty id collections short-circuit to an empty map without an RPC")
    void emptyInputShortCircuits() {
        assertThat(client.activitiesById(List.of())).contains(Map.of());
        assertThat(client.activitiesById(null)).contains(Map.of());
        assertThat(fake.calls).isZero();
    }

    @Test
    @DisplayName("server errors surface as StatusRuntimeException for Resilience4j to record")
    void serverErrorPropagatesAsStatusRuntimeException() {
        // In production the @CircuitBreaker/@Retry aspects catch this (the
        // default config records every exception) and the fallback returns
        // Optional.empty() → snapshot degradation, covered in FeedQueryServiceTest.
        fake.failWith = Status.UNAVAILABLE;

        assertThatThrownBy(() -> client.activitiesById(List.of(6L)))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(e -> assertThat(Status.fromThrowable(e).getCode())
                        .isEqualTo(Status.Code.UNAVAILABLE));
    }

    @Test
    @DisplayName("every call carries the ~3s deadline")
    void callsCarryDeadline() {
        fake.store.put(6L, ActivityPayload.newBuilder()
                .setId(6L).setUserId(1L).setTitle("Park Run").setActivityType("RUN").setIsPrivate(false)
                .build());

        client.activitiesById(List.of(6L));

        assertThat(fake.observedDeadline).isNotNull();
        long remainingMillis = fake.observedDeadline.timeRemaining(TimeUnit.MILLISECONDS);
        assertThat(remainingMillis).isBetween(0L, 3000L);
    }

    /** Fake server: answers from {@code store}, silently skipping unknown ids, or fails with {@code failWith}. */
    private static final class FakeActivityInternalService
            extends ActivityInternalServiceGrpc.ActivityInternalServiceImplBase {

        final Map<Long, ActivityPayload> store = new LinkedHashMap<>();
        Status failWith;
        int calls;
        List<Long> lastRequestedIds;
        Deadline observedDeadline;

        @Override
        public void getActivities(GetActivitiesRequest request,
                                  StreamObserver<GetActivitiesResponse> responseObserver) {
            calls++;
            lastRequestedIds = request.getIdsList();
            observedDeadline = Context.current().getDeadline();
            if (failWith != null) {
                responseObserver.onError(failWith.asRuntimeException());
                return;
            }
            GetActivitiesResponse.Builder response = GetActivitiesResponse.newBuilder();
            for (Long id : request.getIdsList()) {
                ActivityPayload payload = store.get(id);
                if (payload != null) {
                    response.addActivities(payload);
                }
            }
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        }
    }
}
