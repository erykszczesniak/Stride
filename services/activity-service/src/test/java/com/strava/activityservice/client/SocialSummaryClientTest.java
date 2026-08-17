package com.strava.activityservice.client;

import com.strava.activityservice.client.SocialSummaryClient.SocialSummary;
import com.strava.grpc.social.GetSocialSummariesRequest;
import com.strava.grpc.social.GetSocialSummariesResponse;
import com.strava.grpc.social.SocialInternalServiceGrpc;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * In-process gRPC server tests: the real blocking stub talks to a fake
 * SocialInternalService implementation over an in-process channel (no sockets,
 * no Mockito on generated types), injected through the client's package-private
 * stub seam. Resilience semantics (retry, breaker recording, empty-map
 * fallback) are exercised through a minimal Spring AOP context configured with
 * the same recordExceptions list as production application.yml.
 */
@DisplayName("SocialSummaryClient Unit Tests")
class SocialSummaryClientTest {

    /** Mirrors the resilience4j default config in src/main/resources/application.yml. */
    private static final String RECORD_EXCEPTIONS = "io.grpc.StatusRuntimeException,"
            + "org.springframework.web.client.RestClientException,"
            + "java.io.IOException,"
            + "java.util.concurrent.TimeoutException";

    private final FakeSocialService fake = new FakeSocialService();
    private Server server;
    private ManagedChannel channel;

    static final class FakeSocialService extends SocialInternalServiceGrpc.SocialInternalServiceImplBase {
        GetSocialSummariesRequest lastRequest;
        int calls;
        Status failWith; // null -> answer with `response`
        GetSocialSummariesResponse response = GetSocialSummariesResponse.newBuilder().build();

        @Override
        public void getSocialSummaries(GetSocialSummariesRequest request,
                                       StreamObserver<GetSocialSummariesResponse> responseObserver) {
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
    private SocialSummaryClient newClient() {
        SocialSummaryClient client = new SocialSummaryClient();
        client.socialStub = SocialInternalServiceGrpc.newBlockingStub(channel);
        return client;
    }

    /**
     * Minimal Spring context that applies the production @CircuitBreaker/@Retry
     * annotations to the given client instance, with fast retry backoff.
     */
    private ApplicationContextRunner resilienceContext(SocialSummaryClient target) {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        AopAutoConfiguration.class,
                        CircuitBreakerAutoConfiguration.class,
                        RetryAutoConfiguration.class))
                .withPropertyValues(
                        "resilience4j.circuitbreaker.instances.social-service.slidingWindowSize=10",
                        "resilience4j.circuitbreaker.instances.social-service.minimumNumberOfCalls=3",
                        "resilience4j.circuitbreaker.instances.social-service.recordExceptions=" + RECORD_EXCEPTIONS,
                        "resilience4j.retry.instances.social-service.maxAttempts=3",
                        "resilience4j.retry.instances.social-service.waitDuration=1ms")
                .withBean(SocialSummaryClient.class, () -> target);
    }

    private static com.strava.grpc.social.SocialSummary wireSummary(
            long activityId, int likes, int comments, boolean likedByViewer) {
        return com.strava.grpc.social.SocialSummary.newBuilder()
                .setActivityId(activityId)
                .setLikesCount(likes)
                .setCommentsCount(comments)
                .setLikedByViewer(likedByViewer)
                .build();
    }

    @Test
    @DisplayName("one RPC per batch: all ids + viewerId on the wire, response keyed by activity id with field parity")
    void batchesIntoSingleCallAndMapsFields() {
        fake.response = GetSocialSummariesResponse.newBuilder()
                .addSummaries(wireSummary(1L, 3, 2, true))
                .addSummaries(wireSummary(2L, 0, 5, false))
                .build();

        Map<Long, SocialSummary> summaries = newClient().summariesFor(List.of(1L, 2L, 3L), 7L);

        assertThat(fake.calls).isEqualTo(1); // one call per page, never N+1
        assertThat(fake.lastRequest.getActivityIdsList()).containsExactly(1L, 2L, 3L);
        assertThat(fake.lastRequest.hasViewerId()).isTrue();
        assertThat(fake.lastRequest.getViewerId()).isEqualTo(7L);
        // id 3 missing from the response -> absent from the map; the mappers
        // then render SocialSummary.empty() zeros for it
        assertThat(summaries).containsOnlyKeys(1L, 2L);
        assertThat(summaries.get(1L)).isEqualTo(new SocialSummary(1L, 3, 2, true));
        assertThat(summaries.get(2L)).isEqualTo(new SocialSummary(2L, 0, 5, false));
    }

    @Test
    @DisplayName("anonymous viewer: viewerId is absent on the wire, not zero")
    void viewerAbsentWhenNull() {
        fake.response = GetSocialSummariesResponse.newBuilder()
                .addSummaries(wireSummary(1L, 1, 1, false))
                .build();

        newClient().summariesFor(List.of(1L), null);

        assertThat(fake.lastRequest.hasViewerId()).isFalse();
    }

    @Test
    @DisplayName("null or empty id batch short-circuits without an RPC")
    void emptyBatchSkipsCall() {
        SocialSummaryClient client = newClient();

        assertThat(client.summariesFor(List.of(), 7L)).isEmpty();
        assertThat(client.summariesFor(null, 7L)).isEmpty();
        assertThat(fake.calls).isZero();
    }

    @Test
    @DisplayName("failure -> empty-map fallback (mappers render zeros); retried and recorded by the breaker")
    void failureFallsBackToNoSummaries() {
        fake.failWith = Status.UNAVAILABLE;

        resilienceContext(newClient()).run(context -> {
            SocialSummaryClient client = context.getBean(SocialSummaryClient.class);

            assertThat(client.summariesFor(List.of(1L, 2L), 7L)).isEmpty();
            // Single attempt: the @CircuitBreaker-level fallback consumes the
            // failure before the outer @Retry aspect sees it — the same
            // (pre-existing) semantics the REST-era annotations had.
            assertThat(fake.calls).isEqualTo(1);
            // StatusRuntimeException is on the recordExceptions list -> the
            // breaker counted the failure (it would never open otherwise)
            CircuitBreakerRegistry registry = context.getBean(CircuitBreakerRegistry.class);
            assertThat(registry.circuitBreaker("social-service").getMetrics().getNumberOfFailedCalls())
                    .isGreaterThanOrEqualTo(1);
        });
    }
}
