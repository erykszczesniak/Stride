package com.strava.socialservice.feed;

import com.strava.socialservice.client.ActivityBatchClient;
import com.strava.socialservice.client.InternalActivityDto;
import com.strava.socialservice.common.HeaderResponse;
import com.strava.socialservice.domain.ActivityLikeRepository;
import com.strava.socialservice.domain.CommentRepository;
import com.strava.socialservice.domain.FeedItem;
import com.strava.socialservice.domain.FeedItemRepository;
import com.strava.socialservice.domain.UserFeedEntry;
import com.strava.socialservice.domain.UserFeedEntryRepository;
import com.strava.socialservice.user.BaseUserResponse;
import com.strava.socialservice.user.UserCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * CQRS read side of the activity feed, mirroring main's
 * ActivityFeedQueryServiceImpl + ActivityMapper composition:
 *
 *   1. page over the read model (user_feed_entries for /feed, the public
 *      feed_items snapshot for /public);
 *   2. hydrate full activity payloads in ONE batch call to activity-service's
 *      internal API, preserving read-model order;
 *   3. merge locally-owned social state: author (user_cache), likesCount /
 *      commentsCount (indexed COUNTs), likedByCurrentUser (activity_likes).
 *
 * When activity-service is unreachable (circuit open), rows degrade to the
 * local feed_items snapshot: id/title/type/distance/duration/elevation/
 * startTime/privacy survive, while detail-only fields (description, route,
 * photos, HR, calories, splits) come back null. Tradeoff: the feed stays up
 * and scrollable during an activity-service outage at the cost of leaner
 * cards — the frontend renders null fields as absent, so nothing breaks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedQueryService {

    private final UserFeedEntryRepository feedEntryRepository;
    private final FeedItemRepository feedItemRepository;
    private final CommentRepository commentRepository;
    private final ActivityLikeRepository likeRepository;
    private final UserCacheService userCacheService;
    private final ActivityBatchClient activityBatchClient;

    /** GET /api/v1/activities/feed — the viewer's fanned-out feed, newest activity first. */
    @Transactional(readOnly = true)
    public HeaderResponse<ActivityResponse> getFeed(Long userId, Pageable pageable) {
        Page<UserFeedEntry> page = feedEntryRepository
                .findByRecipientUserIdOrderByActivityCreatedAtDescIdDesc(userId, pageable);
        List<Long> orderedIds = page.getContent().stream()
                .map(UserFeedEntry::getActivityId)
                .toList();
        return HeaderResponse.of(hydrate(orderedIds, userId),
                page.getTotalPages(), page.getTotalElements());
    }

    /** GET /api/v1/activities/public — every public activity, newest start first. */
    @Transactional(readOnly = true)
    public HeaderResponse<ActivityResponse> getPublicActivities(Long viewerId, Pageable pageable) {
        Page<FeedItem> page = feedItemRepository.findByIsPrivateFalseOrderByStartedAtDesc(pageable);
        List<Long> orderedIds = page.getContent().stream()
                .map(FeedItem::getActivityId)
                .toList();
        return HeaderResponse.of(hydrate(orderedIds, viewerId),
                page.getTotalPages(), page.getTotalElements());
    }

    private List<ActivityResponse> hydrate(List<Long> orderedIds, Long viewerId) {
        if (orderedIds.isEmpty()) {
            return List.of();
        }

        Map<Long, FeedItem> snapshots = feedItemRepository.findAllByActivityIdIn(orderedIds).stream()
                .collect(Collectors.toMap(FeedItem::getActivityId, Function.identity()));
        Optional<Map<Long, InternalActivityDto>> hydrated = activityBatchClient.activitiesById(orderedIds);

        Map<Long, Integer> likeCounts = toCountMap(likeRepository.countByActivityIdIn(orderedIds));
        Map<Long, Integer> commentCounts = toCountMap(commentRepository.countByActivityIdIn(orderedIds));
        Set<Long> likedByViewer = viewerId == null ? Set.of()
                : new HashSet<>(likeRepository.findActivityIdsLikedBy(viewerId, orderedIds));

        Map<Long, BaseUserResponse> authors = userCacheService.usersFor(
                orderedIds.stream()
                        .map(id -> hydrated.map(m -> m.get(id)).map(InternalActivityDto::userId)
                                .orElseGet(() -> snapshots.containsKey(id) ? snapshots.get(id).getUserId() : null))
                        .filter(java.util.Objects::nonNull)
                        .toList());

        List<ActivityResponse> rows = new ArrayList<>(orderedIds.size());
        for (Long activityId : orderedIds) {
            ActivityResponse row;
            if (hydrated.isPresent()) {
                InternalActivityDto dto = hydrated.get().get(activityId);
                if (dto == null) {
                    // activity-service answered but doesn't know the id: the
                    // activity was deleted and our projection lags — drop the
                    // row, exactly like main drops feed entries whose Activity
                    // lookup comes back empty.
                    continue;
                }
                row = fromInternal(dto);
            } else {
                FeedItem snapshot = snapshots.get(activityId);
                if (snapshot == null) {
                    continue;
                }
                row = fromSnapshot(snapshot);
            }
            row.setLikesCount(likeCounts.getOrDefault(activityId, 0));
            row.setCommentsCount(commentCounts.getOrDefault(activityId, 0));
            row.setLikedByCurrentUser(likedByViewer.contains(activityId));
            row.setUser(authors.get(row.getUser() != null ? row.getUser().getId() : null));
            rows.add(row);
        }
        return rows;
    }

    /** Full-fidelity row from the internal batch payload (everything main's feed rows carry, except gear). */
    private ActivityResponse fromInternal(InternalActivityDto dto) {
        ActivityResponse response = new ActivityResponse();
        response.setId(dto.id());
        response.setTitle(dto.title());
        response.setDescription(dto.description());
        response.setActivityType(parseType(dto.activityType()));
        response.setDistance(dto.distance());
        response.setDuration(dto.duration());
        response.setElevationGain(dto.elevationGain());
        response.setCalories(dto.calories());
        response.setAverageSpeed(dto.averageSpeed());
        response.setStartTime(dto.startTime());
        response.setStartLocation(dto.startLocation());
        response.setPrivate(dto.isPrivate());
        response.setCreatedAt(dto.createdAt());
        response.setRoutePolyline(dto.routePolyline());
        response.setElevationProfile(dto.elevationProfile());
        response.setPointTimestamps(dto.pointTimestamps());
        response.setAverageHeartRate(dto.averageHeartRate());
        response.setMaxHeartRate(dto.maxHeartRate());
        response.setPhotos(dto.photos());
        response.setUser(authorPlaceholder(dto.userId()));
        return response;
    }

    /** Degraded row from the local snapshot — served when activity-service is down. */
    private ActivityResponse fromSnapshot(FeedItem snapshot) {
        ActivityResponse response = new ActivityResponse();
        response.setId(snapshot.getActivityId());
        response.setTitle(snapshot.getTitle());
        response.setActivityType(parseType(snapshot.getSport()));
        response.setDistance(snapshot.getDistanceMeters() != null
                ? snapshot.getDistanceMeters() / 1000.0 : null);
        response.setDuration(snapshot.getDurationSeconds() != null
                ? snapshot.getDurationSeconds().intValue() : null);
        response.setElevationGain(snapshot.getElevationGainMeters() != null
                ? (int) Math.round(snapshot.getElevationGainMeters()) : null);
        response.setStartTime(snapshot.getStartedAt());
        response.setPrivate(snapshot.isPrivate());
        response.setCreatedAt(snapshot.getOccurredAt());
        response.setUser(authorPlaceholder(snapshot.getUserId()));
        return response;
    }

    /** Marker carrying only the author id until the batch user_cache merge replaces it. */
    private BaseUserResponse authorPlaceholder(Long userId) {
        BaseUserResponse user = new BaseUserResponse();
        user.setId(userId);
        return user;
    }

    private static ActivityType parseType(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return ActivityType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return ActivityType.OTHER;
        }
    }

    private static Map<Long, Integer> toCountMap(Collection<Object[]> grouped) {
        Map<Long, Integer> counts = new HashMap<>();
        for (Object[] row : grouped) {
            counts.put((Long) row[0], ((Long) row[1]).intValue());
        }
        return counts;
    }
}
