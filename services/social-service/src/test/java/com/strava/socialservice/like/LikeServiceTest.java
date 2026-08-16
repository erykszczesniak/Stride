package com.strava.socialservice.like;

import com.strava.socialservice.domain.ActivityLike;
import com.strava.socialservice.domain.ActivityLikeRepository;
import com.strava.socialservice.domain.FeedItem;
import com.strava.socialservice.domain.FeedItemRepository;
import com.strava.socialservice.event.SocialEventPublisher;
import com.strava.socialservice.exception.ApiRequestException;
import com.strava.socialservice.user.UserCacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static com.strava.socialservice.constants.ErrorMessage.*;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LikeService")
class LikeServiceTest {

    @Mock private ActivityLikeRepository likeRepository;
    @Mock private FeedItemRepository feedItemRepository;
    @Mock private UserCacheService userCacheService;
    @Mock private SocialEventPublisher eventPublisher;

    @InjectMocks private LikeService service;

    private static final Long ACTIVITY_ID = 7L;
    private static final Long OWNER_ID = 2L;
    private static final Long LIKER_ID = 1L;

    private FeedItem snapshot(Long ownerId) {
        return FeedItem.builder().activityId(ACTIVITY_ID).userId(ownerId).sport("RUN").build();
    }

    @Nested
    @DisplayName("likeActivity")
    class Like {

        @Test
        @DisplayName("stores the like and publishes ActivityLikedEvent with owner + liker display name")
        void likesAndPublishes() {
            when(feedItemRepository.findByActivityId(ACTIVITY_ID)).thenReturn(Optional.of(snapshot(OWNER_ID)));
            when(likeRepository.existsByActivityIdAndUserId(ACTIVITY_ID, LIKER_ID)).thenReturn(false);
            when(userCacheService.displayNameFor(LIKER_ID)).thenReturn("Pan Szczesniak");

            service.likeActivity(ACTIVITY_ID, LIKER_ID);

            verify(likeRepository).save(any(ActivityLike.class));
            verify(eventPublisher).publishActivityLiked(ACTIVITY_ID, OWNER_ID, LIKER_ID, "Pan Szczesniak");
        }

        @Test
        @DisplayName("404s with main's text when the activity is unknown")
        void unknownActivity404s() {
            when(feedItemRepository.findByActivityId(ACTIVITY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.likeActivity(ACTIVITY_ID, LIKER_ID))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(ACTIVITY_NOT_FOUND)
                    .extracting(e -> ((ApiRequestException) e).getStatus())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("double like is a 400 with main's 'Activity already liked' text")
        void doubleLike400s() {
            when(feedItemRepository.findByActivityId(ACTIVITY_ID)).thenReturn(Optional.of(snapshot(OWNER_ID)));
            when(likeRepository.existsByActivityIdAndUserId(ACTIVITY_ID, LIKER_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.likeActivity(ACTIVITY_ID, LIKER_ID))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(ACTIVITY_ALREADY_LIKED)
                    .extracting(e -> ((ApiRequestException) e).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
            verify(likeRepository, never()).save(any());
        }

        @Test
        @DisplayName("skips the event (but keeps the like) when the snapshot has no owner")
        void ownerUnknownSkipsPublish() {
            when(feedItemRepository.findByActivityId(ACTIVITY_ID)).thenReturn(Optional.of(snapshot(null)));
            when(likeRepository.existsByActivityIdAndUserId(ACTIVITY_ID, LIKER_ID)).thenReturn(false);

            assertThatCode(() -> service.likeActivity(ACTIVITY_ID, LIKER_ID)).doesNotThrowAnyException();

            verify(likeRepository).save(any(ActivityLike.class));
            verify(eventPublisher, never()).publishActivityLiked(anyLong(), anyLong(), anyLong(), anyString());
        }
    }

    @Nested
    @DisplayName("unlikeActivity")
    class Unlike {

        @Test
        @DisplayName("removes the like")
        void unlikes() {
            when(feedItemRepository.findByActivityId(ACTIVITY_ID)).thenReturn(Optional.of(snapshot(OWNER_ID)));
            when(likeRepository.existsByActivityIdAndUserId(ACTIVITY_ID, LIKER_ID)).thenReturn(true);

            service.unlikeActivity(ACTIVITY_ID, LIKER_ID);

            verify(likeRepository).deleteByActivityIdAndUserId(ACTIVITY_ID, LIKER_ID);
        }

        @Test
        @DisplayName("404s when the activity is unknown")
        void unknownActivity404s() {
            when(feedItemRepository.findByActivityId(ACTIVITY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.unlikeActivity(ACTIVITY_ID, LIKER_ID))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(ACTIVITY_NOT_FOUND);
        }

        @Test
        @DisplayName("unliking something never liked is a 400 with main's 'Activity not liked' text")
        void notLiked400s() {
            when(feedItemRepository.findByActivityId(ACTIVITY_ID)).thenReturn(Optional.of(snapshot(OWNER_ID)));
            when(likeRepository.existsByActivityIdAndUserId(ACTIVITY_ID, LIKER_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.unlikeActivity(ACTIVITY_ID, LIKER_ID))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(ACTIVITY_NOT_LIKED)
                    .extracting(e -> ((ApiRequestException) e).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
            verify(likeRepository, never()).deleteByActivityIdAndUserId(anyLong(), anyLong());
        }
    }
}
