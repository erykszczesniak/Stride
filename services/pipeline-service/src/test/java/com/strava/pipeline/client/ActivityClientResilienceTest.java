package com.strava.pipeline.client;

import com.strava.grpc.activity.ActivityInternalServiceGrpc;
import com.strava.grpc.activity.ActivityPayload;
import com.strava.grpc.activity.GetActivitiesResponse;
import com.strava.pipeline.client.ActivityClientGrpcTest.FakeActivityInternal;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.AopTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the Phase 23c cutover kept the REST-era failure semantics on the
 * Resilience4j instance {@code activity-service}: a gRPC
 * StatusRuntimeException is RECORDED by the circuit breaker (the module
 * config ignores ApiRequestException only) and the fallback degrades the FTP
 * power enrichment to an empty map — never an error surfaced to the caller.
 *
 * The client bean is proxied by the real Resilience4j aspects (minimal
 * context: CB + Retry auto-configuration and AspectJ auto-proxying, with the
 * module's ignoreExceptions mirrored below); the downstream is a fake
 * ImplBase on an in-process server — generated stubs are never mocked.
 */
@SpringJUnitConfig(ActivityClientResilienceTest.Config.class)
@TestPropertySource(properties = {
        "resilience4j.circuitbreaker.instances.activity-service.slidingWindowSize=10",
        "resilience4j.circuitbreaker.instances.activity-service.minimumNumberOfCalls=10",
        "resilience4j.circuitbreaker.instances.activity-service.ignoreExceptions="
                + "com.strava.pipeline.exception.ApiRequestException",
        "resilience4j.retry.instances.activity-service.maxAttempts=3",
        "resilience4j.retry.instances.activity-service.waitDuration=10ms",
        "resilience4j.retry.instances.activity-service.ignoreExceptions="
                + "com.strava.pipeline.exception.ApiRequestException"
})
@DisplayName("ActivityClient Resilience4j Semantics Tests")
class ActivityClientResilienceTest {

    @Configuration
    @EnableAspectJAutoProxy
    @ImportAutoConfiguration({CircuitBreakerAutoConfiguration.class, RetryAutoConfiguration.class})
    static class Config {

        @Bean
        ActivityClient activityClient() {
            return new ActivityClient(new RestTemplate());
        }
    }

    @Autowired
    private ActivityClient activityClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private FakeActivityInternal fake;
    private Server server;
    private ManagedChannel channel;

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
        // The autowired bean is the AOP proxy; the stub seam lives on the target.
        ActivityClient target = AopTestUtils.getUltimateTargetObject(activityClient);
        target.setActivityInternal(ActivityInternalServiceGrpc.newBlockingStub(channel));
    }

    @AfterEach
    void tearDown() throws Exception {
        channel.shutdownNow();
        server.shutdownNow();
        channel.awaitTermination(5, TimeUnit.SECONDS);
        server.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("StatusRuntimeException is recorded by the circuit breaker and degrades to an empty map")
    void statusErrorIsRecordedAndDegrades() {
        CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker("activity-service");
        int failedBefore = breaker.getMetrics().getNumberOfFailedCalls();
        fake.errorStatus = Status.UNAVAILABLE.withDescription("activity-service down");

        Map<Long, Integer> powers = activityClient.averagePowerByIds(List.of(7L));

        // Degradation, not an error: FTP enrichment reports "no power data" (0).
        assertThat(powers).isEmpty();
        assertThat(fake.calls).isGreaterThanOrEqualTo(1);
        // Recorded: StatusRuntimeException is not in ignoreExceptions
        // (ApiRequestException only), so it counts toward the failure rate.
        assertThat(breaker.getMetrics().getNumberOfFailedCalls())
                .isGreaterThan(failedBefore);
    }

    @Test
    @DisplayName("successful call passes through the resilience proxy unchanged")
    void successPassesThroughProxy() {
        CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker("activity-service");
        int successBefore = breaker.getMetrics().getNumberOfSuccessfulCalls();
        fake.response = GetActivitiesResponse.newBuilder()
                .addActivities(ActivityPayload.newBuilder()
                        .setId(5L).setUserId(9L).setTitle("Tempo")
                        .setActivityType("CYCLING").setAveragePower(250)
                        .build())
                .build();

        Map<Long, Integer> powers = activityClient.averagePowerByIds(List.of(5L));

        assertThat(powers).containsExactlyEntriesOf(Map.of(5L, 250));
        assertThat(breaker.getMetrics().getNumberOfSuccessfulCalls())
                .isGreaterThan(successBefore);
    }
}
