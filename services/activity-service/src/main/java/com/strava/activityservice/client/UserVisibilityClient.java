package com.strava.activityservice.client;

import com.strava.grpc.user.GetVisibilityRequest;
import com.strava.grpc.user.GetVisibilityResponse;
import com.strava.grpc.user.UserInternalServiceGrpc;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Owner-profile privacy + follow facts from user-service's cluster-internal
 * gRPC API ({@code UserInternalService.GetVisibility}), resolved through
 * Eureka ({@code discovery:///user-service}). Every call carries a short
 * deadline and is wrapped in Resilience4j retry + circuit breaker (instance
 * "user-service"; StatusRuntimeException is in the recordExceptions list so
 * gRPC failures count toward opening the breaker).
 *
 * Fallback tradeoff: on failure we report {isPrivate:false, viewerFollows:false},
 * i.e. we treat the owner's PROFILE as not private. Failing closed is
 * impossible without the data — it would blank every profile page and feed
 * card whenever user-service blips, hiding the 99% public content to protect
 * the rare private profile. Activity-level isPrivate is stored locally and
 * stays enforced regardless, so a private ACTIVITY never leaks; only the
 * profile-wide privacy screen degrades during an outage.
 *
 * An unknown owner surfaces as gRPC NOT_FOUND (where the REST era answered
 * 404): the StatusRuntimeException falls through to the same fail-open
 * fallback, exactly as the 404 did.
 */
@Slf4j
@Component
public class UserVisibilityClient {

    private static final long DEADLINE_SECONDS = 2;

    /**
     * Package-private test seam. In production the net.devh starter injects a
     * blocking stub on the discovery-resolved channel; tests assign a stub
     * bound to an in-process server.
     */
    @GrpcClient("user-service")
    UserInternalServiceGrpc.UserInternalServiceBlockingStub userStub;

    public record VisibilityInfo(boolean isPrivate, boolean viewerFollows) {

        public static final VisibilityInfo FALLBACK = new VisibilityInfo(false, false);
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallback")
    @Retry(name = "user-service")
    public VisibilityInfo visibilityFor(Long ownerId, Long viewerId) {
        GetVisibilityRequest.Builder request = GetVisibilityRequest.newBuilder().setUserId(ownerId);
        if (viewerId != null) {
            request.setViewerId(viewerId);
        }
        GetVisibilityResponse response = userStub
                .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
                .getVisibility(request.build());
        return new VisibilityInfo(response.getIsPrivate(), response.getViewerFollows());
    }

    @SuppressWarnings("unused")
    private VisibilityInfo fallback(Long ownerId, Long viewerId, Throwable ex) {
        log.warn("UserVisibilityClient fallback for ownerId={} viewerId={} (cause: {})",
                ownerId, viewerId, ex.getMessage());
        return VisibilityInfo.FALLBACK;
    }
}
