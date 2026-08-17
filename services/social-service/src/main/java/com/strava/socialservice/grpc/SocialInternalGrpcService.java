package com.strava.socialservice.grpc;

import com.strava.grpc.social.GetSocialSummariesRequest;
import com.strava.grpc.social.GetSocialSummariesResponse;
import com.strava.grpc.social.SocialInternalServiceGrpc;
import com.strava.grpc.social.SocialSummary;
import com.strava.socialservice.internal.SocialSummaryService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * CLUSTER-INTERNAL gRPC transport for the social-summary contract — the
 * strongly-typed successor of the REST endpoint {@code
 * /internal/social/activities}. Listens on the dedicated gRPC port (not the
 * HTTP port), which the gateway does not route, so it is unreachable from
 * outside the cluster.
 *
 * Semantics are shared with the REST endpoint via {@link SocialSummaryService}:
 * every requested id gets a row (zero-filled when the activity has no social
 * state) and {@code liked_by_viewer} is only computed when {@code viewer_id}
 * is set on the request.
 */
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class SocialInternalGrpcService extends SocialInternalServiceGrpc.SocialInternalServiceImplBase {

    private final SocialSummaryService summaryService;

    @Override
    public void getSocialSummaries(GetSocialSummariesRequest request,
                                   StreamObserver<GetSocialSummariesResponse> responseObserver) {
        try {
            Long viewerId = request.hasViewerId() ? request.getViewerId() : null;
            GetSocialSummariesResponse.Builder response = GetSocialSummariesResponse.newBuilder();
            summaryService.getSummaries(request.getActivityIdsList(), viewerId).forEach(summary ->
                    response.addSummaries(SocialSummary.newBuilder()
                            .setActivityId(summary.activityId())
                            .setLikesCount(summary.likesCount())
                            .setCommentsCount(summary.commentsCount())
                            .setLikedByViewer(summary.likedByViewer())
                            .build()));
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("GetSocialSummaries failed for {} activity ids", request.getActivityIdsCount(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Unexpected error computing social summaries")
                    .asRuntimeException());
        }
    }
}
