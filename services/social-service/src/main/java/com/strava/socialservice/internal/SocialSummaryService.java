package com.strava.socialservice.internal;

import com.strava.socialservice.domain.ActivityLikeRepository;
import com.strava.socialservice.domain.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Single source of truth for the cluster-internal social-summary contract,
 * shared by the REST endpoint ({@link InternalSocialController}) and its gRPC
 * successor ({@code SocialInternalGrpcService}).
 *
 * Semantics both transports rely on:
 * <ul>
 *   <li>every requested id gets a row, zero-filled when the activity has no
 *       social state (duplicates collapsed, request order preserved);</li>
 *   <li>{@code likedByViewer} is only looked up when a viewer id is present —
 *       without one it is always {@code false} and no query is issued.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class SocialSummaryService {

    private final ActivityLikeRepository likeRepository;
    private final CommentRepository commentRepository;

    public record Summary(long activityId, int likesCount, int commentsCount, boolean likedByViewer) {}

    @Transactional(readOnly = true)
    public List<Summary> getSummaries(List<Long> ids, Long viewerId) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, Integer> likeCounts = toCountMap(likeRepository.countByActivityIdIn(ids));
        Map<Long, Integer> commentCounts = toCountMap(commentRepository.countByActivityIdIn(ids));
        Set<Long> likedByViewer = viewerId == null ? Set.of()
                : new HashSet<>(likeRepository.findActivityIdsLikedBy(viewerId, ids));

        return ids.stream().distinct()
                .map(id -> new Summary(
                        id,
                        likeCounts.getOrDefault(id, 0),
                        commentCounts.getOrDefault(id, 0),
                        likedByViewer.contains(id)))
                .toList();
    }

    private static Map<Long, Integer> toCountMap(List<Object[]> grouped) {
        Map<Long, Integer> counts = new HashMap<>();
        for (Object[] row : grouped) {
            counts.put((Long) row[0], ((Long) row[1]).intValue());
        }
        return counts;
    }
}
