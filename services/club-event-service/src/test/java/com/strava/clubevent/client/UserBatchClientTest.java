package com.strava.clubevent.client;

import com.strava.clubevent.common.BaseUserResponse;
import com.strava.grpc.user.GetUserSummariesRequest;
import com.strava.grpc.user.GetUserSummariesResponse;
import com.strava.grpc.user.UserInternalServiceGrpc;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the gRPC cutover of the user batch lookup: one
 * {@code GetUserSummaries} call with de-duplicated ids, mapped into the same
 * id-keyed map member lists and owner blocks always consumed (absent proto3
 * optionals → nulls; ids the server skipped stay absent so callers fall back
 * to the "User #id" placeholder). Generated stubs are final, so the test runs
 * against a fake ImplBase behind an in-process server.
 */
@DisplayName("UserBatchClient Unit Tests")
class UserBatchClientTest {

    private Server server;
    private ManagedChannel channel;
    private UserBatchClient userBatchClient;

    private final AtomicReference<GetUserSummariesRequest> lastRequest = new AtomicReference<>();
    private GetUserSummariesResponse cannedResponse = GetUserSummariesResponse.getDefaultInstance();
    private Status cannedError;

    private final UserInternalServiceGrpc.UserInternalServiceImplBase fakeService =
            new UserInternalServiceGrpc.UserInternalServiceImplBase() {
                @Override
                public void getUserSummaries(GetUserSummariesRequest request,
                                             StreamObserver<GetUserSummariesResponse> responseObserver) {
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
        userBatchClient = new UserBatchClient(UserInternalServiceGrpc.newBlockingStub(channel));
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    @DisplayName("Should return an empty map without calling user-service for null or empty ids")
    void shouldShortCircuitOnNullOrEmptyIds() {
        assertThat(userBatchClient.usersByIds(null)).isEmpty();
        assertThat(userBatchClient.usersByIds(List.of())).isEmpty();
        assertThat(lastRequest.get()).isNull();
    }

    @Test
    @DisplayName("Should de-duplicate ids and map summaries by user id")
    void shouldDeduplicateIdsAndMapSummaries() {
        cannedResponse = GetUserSummariesResponse.newBuilder()
                .addUsers(com.strava.grpc.user.UserSummary.newBuilder()
                        .setId(5L).setFirstName("Ada").setLastName("Lovelace")
                        .setFullName("Ada Lovelace").setAvatar("http://cdn/ada.png"))
                .addUsers(com.strava.grpc.user.UserSummary.newBuilder()
                        .setId(7L).setFirstName("Grace").setLastName("Hopper")
                        .setFullName("Grace Hopper"))
                .build();

        Map<Long, UserSummary> users = userBatchClient.usersByIds(List.of(5L, 7L, 5L));

        assertThat(lastRequest.get().getIdsList()).containsExactly(5L, 7L);
        assertThat(users).hasSize(2);
        BaseUserResponse ada = users.get(5L).toBaseUserResponse();
        assertThat(ada.getFirstName()).isEqualTo("Ada");
        assertThat(ada.getLastName()).isEqualTo("Lovelace");
        assertThat(ada.getAvatar()).isEqualTo("http://cdn/ada.png");
        assertThat(users.get(7L).avatar()).isNull();
    }

    @Test
    @DisplayName("Should map absent optional fields to nulls, matching the old JSON shape")
    void shouldMapAbsentOptionalsToNulls() {
        cannedResponse = GetUserSummariesResponse.newBuilder()
                .addUsers(com.strava.grpc.user.UserSummary.newBuilder().setId(9L))
                .build();

        UserSummary summary = userBatchClient.usersByIds(List.of(9L)).get(9L);

        assertThat(summary.firstName()).isNull();
        assertThat(summary.lastName()).isNull();
        assertThat(summary.fullName()).isNull();
        assertThat(summary.avatar()).isNull();
    }

    @Test
    @DisplayName("Should omit unknown ids so callers render the 'User #id' placeholder")
    void shouldOmitUnknownIdsForPlaceholderRendering() {
        cannedResponse = GetUserSummariesResponse.getDefaultInstance();

        Map<Long, UserSummary> users = userBatchClient.usersByIds(List.of(404L));

        assertThat(users).isEmpty();
        // Callers hit the unknown(id) branch for absent keys — "User #404".
        BaseUserResponse placeholder = UserSummary.unknown(404L);
        assertThat(placeholder.getFirstName()).isEqualTo("User");
        assertThat(placeholder.getLastName()).isEqualTo("#404");
        assertThat(placeholder.getFullName()).isEqualTo("User #404");
    }

    @Test
    @DisplayName("Should surface StatusRuntimeException so the circuit breaker records it")
    void shouldSurfaceStatusRuntimeException() {
        // The Resilience4j proxy (recordExceptions: io.grpc.StatusRuntimeException)
        // turns this into the empty-map fallback in the Spring context; the raw
        // client must let it propagate.
        cannedError = Status.UNAVAILABLE.withDescription("user-service down");

        assertThatThrownBy(() -> userBatchClient.usersByIds(List.of(1L)))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessageContaining("UNAVAILABLE");
    }
}
