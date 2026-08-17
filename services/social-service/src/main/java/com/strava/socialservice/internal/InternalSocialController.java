package com.strava.socialservice.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CLUSTER-INTERNAL API — not routed by the gateway and therefore never
 * reachable from outside; SecurityConfig permits /internal/** because the
 * caller is activity-service's SocialSummaryClient, which carries no user
 * identity headers. In Kubernetes a NetworkPolicy additionally restricts who
 * can reach the pod.
 *
 * The response shape is a cross-service contract, parsed by SocialSummaryClient
 * into records {@code (activityId, likesCount, commentsCount, likedByViewer)} —
 * do not change field names. Every requested id gets a row (zeros when there is
 * no social activity), keeping the client's "missing id falls back to zeros"
 * branch a dead path in practice.
 *
 * The summary semantics live in {@link SocialSummaryService}, shared with the
 * gRPC transport ({@code SocialInternalGrpcService}) so both stay in lockstep.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/social")
public class InternalSocialController {

    private final SocialSummaryService summaryService;

    public record SocialSummaryResponse(Long activityId, int likesCount, int commentsCount, boolean likedByViewer) {}

    @GetMapping("/activities")
    public ResponseEntity<List<SocialSummaryResponse>> getSummaries(
            @RequestParam List<Long> ids,
            @RequestParam(required = false) Long viewerId) {
        List<SocialSummaryResponse> payload = summaryService.getSummaries(ids, viewerId).stream()
                .map(s -> new SocialSummaryResponse(
                        s.activityId(), s.likesCount(), s.commentsCount(), s.likedByViewer()))
                .toList();
        return ResponseEntity.ok(payload);
    }
}
