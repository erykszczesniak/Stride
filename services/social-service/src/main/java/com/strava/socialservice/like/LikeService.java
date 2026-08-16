package com.strava.socialservice.like;

import com.strava.socialservice.domain.ActivityLike;
import com.strava.socialservice.domain.ActivityLikeRepository;
import com.strava.socialservice.domain.FeedItem;
import com.strava.socialservice.domain.FeedItemRepository;
import com.strava.socialservice.event.SocialEventPublisher;
import com.strava.socialservice.exception.ApiRequestException;
import com.strava.socialservice.user.UserCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.strava.socialservice.constants.ErrorMessage.*;

/**
 * Activity like write side, ported from main's ActivityServiceImpl
 * likeActivity/unlikeActivity — same status codes and message texts.
 * Existence is answered by the local feed_items snapshot (eventually
 * consistent; the tiny created-event lag window is the accepted tradeoff for
 * not calling activity-service synchronously on every like).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LikeService {

    private final ActivityLikeRepository likeRepository;
    private final FeedItemRepository feedItemRepository;
    private final UserCacheService userCacheService;
    private final SocialEventPublisher eventPublisher;

    @Transactional
    public void likeActivity(Long activityId, Long userId) {
        FeedItem activity = feedItemRepository.findByActivityId(activityId)
                .orElseThrow(() -> new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (likeRepository.existsByActivityIdAndUserId(activityId, userId)) {
            throw new ApiRequestException(ACTIVITY_ALREADY_LIKED, HttpStatus.BAD_REQUEST);
        }

        likeRepository.save(ActivityLike.builder()
                .activityId(activityId)
                .userId(userId)
                .build());

        Long ownerId = activity.getUserId();
        if (ownerId == null) {
            // Snapshot row without an owner should not happen; if it ever does,
            // an ActivityLikedEvent without a recipient would be undeliverable.
            log.warn("Skipping ActivityLikedEvent for activityId={} — owner unknown in feed_items", activityId);
            return;
        }
        eventPublisher.publishActivityLiked(activityId, ownerId, userId,
                userCacheService.displayNameFor(userId));
    }

    @Transactional
    public void unlikeActivity(Long activityId, Long userId) {
        if (feedItemRepository.findByActivityId(activityId).isEmpty()) {
            throw new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        if (!likeRepository.existsByActivityIdAndUserId(activityId, userId)) {
            throw new ApiRequestException(ACTIVITY_NOT_LIKED, HttpStatus.BAD_REQUEST);
        }
        likeRepository.deleteByActivityIdAndUserId(activityId, userId);
    }
}
