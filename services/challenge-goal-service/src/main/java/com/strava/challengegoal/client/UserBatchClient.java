package com.strava.challengegoal.client;

import com.strava.grpc.user.GetUserSummariesRequest;
import com.strava.grpc.user.GetUserSummariesResponse;
import com.strava.grpc.user.UserInternalServiceGrpc;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Batch-resolves user display data (name + avatar) from user-service via the
 * cluster-internal gRPC endpoint ({@code UserInternalService.GetUserSummaries});
 * unknown ids are silently skipped by the server, mirroring the old REST
 * {@code /internal/users?ids=...} contract. Guarded by Resilience4j (instance
 * "user-service"); the fallback returns an empty map so read endpoints stay up
 * — callers render a "User #id" placeholder for ids the lookup could not
 * resolve. A per-call deadline caps the wait on a sick user-service; the
 * resulting StatusRuntimeException is a recorded circuit-breaker exception.
 */
@Slf4j
@Component
public class UserBatchClient {

    private static final long DEADLINE_SECONDS = 3;

    @GrpcClient("user-service")
    private UserInternalServiceGrpc.UserInternalServiceBlockingStub userStub;

    /** Spring path — the stub is injected into the field by net.devh. */
    UserBatchClient() {
    }

    /** Test seam — lets unit tests hand in a stub bound to an in-process channel. */
    UserBatchClient(UserInternalServiceGrpc.UserInternalServiceBlockingStub userStub) {
        this.userStub = userStub;
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallback")
    @Retry(name = "user-service")
    public Map<Long, UserSummary> usersByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();

        GetUserSummariesRequest request = GetUserSummariesRequest.newBuilder()
                .addAllIds(ids.stream().distinct().toList())
                .build();

        GetUserSummariesResponse response = userStub
                .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
                .getUserSummaries(request);

        return response.getUsersList().stream()
                .map(UserBatchClient::toRecord)
                .collect(Collectors.toMap(UserSummary::id, Function.identity(), (a, b) -> a));
    }

    /** proto3 optional fields map to null when absent, like the old JSON shape. */
    private static UserSummary toRecord(com.strava.grpc.user.UserSummary u) {
        return new UserSummary(
                u.getId(),
                u.hasFirstName() ? u.getFirstName() : null,
                u.hasLastName() ? u.getLastName() : null,
                u.hasFullName() ? u.getFullName() : null,
                u.hasAvatar() ? u.getAvatar() : null);
    }

    @SuppressWarnings("unused")
    private Map<Long, UserSummary> fallback(Collection<Long> ids, Throwable ex) {
        log.warn("UserBatchClient fallback for {} ids (cause: {})",
                ids == null ? 0 : ids.size(), ex.getMessage());
        return Map.of();
    }
}
