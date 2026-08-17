package com.strava.activityservice.client;

import com.strava.grpc.social.GetSocialSummariesRequest;
import com.strava.grpc.social.GetSocialSummariesResponse;
import com.strava.grpc.social.SocialInternalServiceGrpc;
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
 * Batch social counts (likes/comments/liked-by-viewer) from social-service's
 * cluster-internal gRPC API ({@code SocialInternalService.GetSocialSummaries}),
 * one call per response page instead of N+1 per row. The channel is resolved
 * through Eureka ({@code discovery:///social-service}), every call carries a
 * short deadline, and the call is wrapped in Resilience4j retry + circuit
 * breaker (instance "social-service"; StatusRuntimeException is in the
 * recordExceptions list so gRPC failures count toward opening the breaker).
 *
 * The fallback returns no summaries, which the mappers render as
 * zeros/likedByCurrentUser=false — activity endpoints stay up when
 * social-service is down (or, during the migration phases, not yet deployed).
 */
@Slf4j
@Component
public class SocialSummaryClient {

    private static final long DEADLINE_SECONDS = 2;

    /**
     * Package-private test seam. In production the net.devh starter injects a
     * blocking stub on the discovery-resolved channel; tests assign a stub
     * bound to an in-process server.
     */
    @GrpcClient("social-service")
    SocialInternalServiceGrpc.SocialInternalServiceBlockingStub socialStub;

    public record SocialSummary(Long activityId, int likesCount, int commentsCount, boolean likedByViewer) {

        public static SocialSummary empty(Long activityId) {
            return new SocialSummary(activityId, 0, 0, false);
        }
    }

    /** Summaries keyed by activity id; ids missing from the response fall back to zeros. */
    @CircuitBreaker(name = "social-service", fallbackMethod = "fallback")
    @Retry(name = "social-service")
    public Map<Long, SocialSummary> summariesFor(Collection<Long> activityIds, Long viewerId) {
        if (activityIds == null || activityIds.isEmpty()) {
            return Map.of();
        }
        GetSocialSummariesRequest.Builder request = GetSocialSummariesRequest.newBuilder()
                .addAllActivityIds(activityIds);
        if (viewerId != null) {
            request.setViewerId(viewerId);
        }
        GetSocialSummariesResponse response = socialStub
                .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
                .getSocialSummaries(request.build());
        return response.getSummariesList().stream()
                .map(s -> new SocialSummary(
                        s.getActivityId(), s.getLikesCount(), s.getCommentsCount(), s.getLikedByViewer()))
                .collect(Collectors.toMap(SocialSummary::activityId, Function.identity(), (a, b) -> a));
    }

    @SuppressWarnings("unused")
    private Map<Long, SocialSummary> fallback(Collection<Long> activityIds, Long viewerId, Throwable ex) {
        log.warn("SocialSummaryClient fallback for {} ids (cause: {})",
                activityIds == null ? 0 : activityIds.size(), ex.getMessage());
        return Map.of();
    }
}
