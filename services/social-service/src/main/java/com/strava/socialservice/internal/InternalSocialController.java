package com.strava.socialservice.internal;

import com.strava.socialservice.domain.ActivityLikeRepository;
import com.strava.socialservice.domain.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/social")
public class InternalSocialController {

    private final ActivityLikeRepository likeRepository;
    private final CommentRepository commentRepository;

    public record SocialSummaryResponse(Long activityId, int likesCount, int commentsCount, boolean likedByViewer) {}

    @GetMapping("/activities")
    @Transactional(readOnly = true)
    public ResponseEntity<List<SocialSummaryResponse>> getSummaries(
            @RequestParam List<Long> ids,
            @RequestParam(required = false) Long viewerId) {
        if (ids.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        Map<Long, Integer> likeCounts = toCountMap(likeRepository.countByActivityIdIn(ids));
        Map<Long, Integer> commentCounts = toCountMap(commentRepository.countByActivityIdIn(ids));
        Set<Long> likedByViewer = viewerId == null ? Set.of()
                : new HashSet<>(likeRepository.findActivityIdsLikedBy(viewerId, ids));

        List<SocialSummaryResponse> payload = ids.stream().distinct()
                .map(id -> new SocialSummaryResponse(
                        id,
                        likeCounts.getOrDefault(id, 0),
                        commentCounts.getOrDefault(id, 0),
                        likedByViewer.contains(id)))
                .toList();
        return ResponseEntity.ok(payload);
    }

    private static Map<Long, Integer> toCountMap(List<Object[]> grouped) {
        Map<Long, Integer> counts = new HashMap<>();
        for (Object[] row : grouped) {
            counts.put((Long) row[0], ((Long) row[1]).intValue());
        }
        return counts;
    }
}
