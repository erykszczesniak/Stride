package com.strava.activityservice.client;

import com.strava.activityservice.client.UserVisibilityClient.VisibilityInfo;
import com.strava.grpc.user.GetVisibilityRequest;
import com.strava.grpc.user.GetVisibilityResponse;
import com.strava.grpc.user.UserInternalServiceGrpc;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration;
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
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * In-process gRPC server tests: the real blocking stub talks to a fake
 * UserInternalService implementation over an in-process channel (no sockets,
 * no Mockito on generated types), injected through the client's package-private
 * stub seam. Resilience semantics (retry, breaker recording, fail-open
 * fallback) are exercised through a minimal Spring AOP context configured with
 * the same recordExceptions list as production application.yml.
 */
@DisplayName("UserVisibilityClient Unit Tests")
class UserVisibilityClientTest {

    /** Mirrors the resilience4j default config in src/main/resources/application.yml. */
    private static final String RECORD_EXCEPTIONS = "io.grpc.StatusRuntimeException,"
            + "org.springframework.web.client.RestClientException,"
            + "java.io.IOException,"
            + "java.util.concurrent.TimeoutException";

    private final FakeUserService fake = new FakeUserService();
    private Server server;
    private ManagedChannel channel;

    static final class FakeUserService extends UserInternalServiceGrpc.UserInternalServiceImplBase {
        GetVisibilityRequest lastRequest;
        int calls;
        Status failWith; // null -> answer with `response`
        GetVisibilityResponse response = GetVisibilityResponse.newBuilder().build();

        @Override
        public void getVisibility(GetVisibilityRequest request,
                                  StreamObserver<GetVisibilityResponse> responseObserver) {
            calls++;
            lastRequest = request;
            if (failWith != null) {
                responseObserver.onError(failWith.asRuntimeException());
                return;
            }
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        String serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(fake)
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    /** Client wired to the in-process server through the package-private stub seam. */
    private UserVisibilityClient newClient() {
        UserVisibilityClient client = new UserVisibilityClient();
        client.userStub = UserInternalServiceGrpc.newBlockingStub(channel);
        return client;
    }

    /**
     * Minimal Spring context that applies the production @CircuitBreaker/@Retry
     * annotations to the given client instance, with fast retry backoff.
     */
    private ApplicationContextRunner resilienceContext(UserVisibilityClient target) {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        AopAutoConfiguration.class,
                        CircuitBreakerAutoConfiguration.class,
                        RetryAutoConfiguration.class))
                .withPropertyValues(
                        "resilience4j.circuitbreaker.instances.user-service.slidingWindowSize=10",
                        "resilience4j.circuitbreaker.instances.user-service.minimumNumberOfCalls=3",
                        "resilience4j.circuitbreaker.instances.user-service.recordExceptions=" + RECORD_EXCEPTIONS,
                        "resilience4j.retry.instances.user-service.maxAttempts=3",
                        "resilience4j.retry.instances.user-service.waitDuration=1ms")
                .withBean(UserVisibilityClient.class, () -> target);
    }

    @Test
    @DisplayName("maps the wire response onto VisibilityInfo and sends ownerId + viewerId")
    void mapsResponseAndSendsIds() {
        fake.response = GetVisibilityResponse.newBuilder()
                .setIsPrivate(true)
                .setViewerFollows(true)
                .build();

        VisibilityInfo info = newClient().visibilityFor(5L, 7L);

        assertThat(info).isEqualTo(new VisibilityInfo(true, true));
        assertThat(fake.lastRequest.getUserId()).isEqualTo(5L);
        assertThat(fake.lastRequest.hasViewerId()).isTrue();
        assertThat(fake.lastRequest.getViewerId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("anonymous viewer: viewerId is absent on the wire, not zero")
    void viewerAbsentWhenNull() {
        VisibilityInfo info = newClient().visibilityFor(5L, null);

        assertThat(info).isEqualTo(new VisibilityInfo(false, false));
        assertThat(fake.lastRequest.getUserId()).isEqualTo(5L);
        assertThat(fake.lastRequest.hasViewerId()).isFalse();
    }

    @Test
    @DisplayName("without the resilience proxy a server failure surfaces as StatusRuntimeException")
    void rawStubPropagatesStatusRuntimeException() {
        fake.failWith = Status.UNAVAILABLE;

        assertThatThrownBy(() -> newClient().visibilityFor(5L, 7L))
                .isInstanceOf(StatusRuntimeException.class);
    }

    @Test
    @DisplayName("failure -> fail-open fallback {isPrivate:false, viewerFollows:false}; retried and recorded by the breaker")
    void failureFallsOpenViaResilience() {
        fake.failWith = Status.UNAVAILABLE;

        resilienceContext(newClient()).run(context -> {
            UserVisibilityClient client = context.getBean(UserVisibilityClient.class);

            assertThat(client.visibilityFor(5L, 7L)).isEqualTo(VisibilityInfo.FALLBACK);
            // Single attempt: the @CircuitBreaker-level fallback consumes the
            // failure before the outer @Retry aspect sees it — the same
            // (pre-existing) semantics the REST-era annotations had.
            assertThat(fake.calls).isEqualTo(1);
            // StatusRuntimeException is on the recordExceptions list -> the
            // breaker counted the failure (it would never open otherwise)
            CircuitBreakerRegistry registry = context.getBean(CircuitBreakerRegistry.class);
            assertThat(registry.circuitBreaker("user-service").getMetrics().getNumberOfFailedCalls())
                    .isGreaterThanOrEqualTo(1);
        });
    }

    @Test
    @DisplayName("unknown owner (gRPC NOT_FOUND, the old REST 404) -> same fail-open fallback")
    void notFoundFailsOpenLikeRest404() {
        fake.failWith = Status.NOT_FOUND;

        resilienceContext(newClient()).run(context -> {
            UserVisibilityClient client = context.getBean(UserVisibilityClient.class);

            assertThat(client.visibilityFor(999L, null)).isEqualTo(VisibilityInfo.FALLBACK);
        });
    }
}
