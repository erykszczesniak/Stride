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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedQueryService")
class FeedQueryServiceTest {

    @Mock private UserFeedEntryRepository feedEntryRepository;
    @Mock private FeedItemRepository feedItemRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private ActivityLikeRepository likeRepository;
    @Mock private UserCacheService userCacheService;
    @Mock private ActivityBatchClient activityBatchClient;

    @InjectMocks private FeedQueryService service;

    private static final Long VIEWER = 1L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 1, 10, 0);

    private UserFeedEntry entry(Long activityId, Long actorId, LocalDateTime createdAt) {
        return UserFeedEntry.builder()
                .recipientUserId(VIEWER).activityId(activityId).actorUserId(actorId)
                .activityCreatedAt(createdAt).build();
    }

    private FeedItem snapshot(Long activityId, Long userId, String title) {
        return FeedItem.builder()
                .activityId(activityId).userId(userId).sport("RUN").title(title)
                .distanceMeters(8500.0).durationSeconds(2880L).elevationGainMeters(156.0)
                .startedAt(NOW.minusHours(2)).occurredAt(NOW.minusHours(2))
                .build();
    }

    private InternalActivityDto dto(Long id, Long userId, String title) {
        return new InternalActivityDto(id, userId, title, "desc", "RUN",
                8.5, 2880, 156, 620, 10.6, null, 150, 180, null, null,
                NOW.minusHours(2), null, "Warsaw", null, "poly{}", new double[]{1, 2}, new long[]{0, 60},
                null, false, List.of("p.jpg"), NOW.minusHours(2), null);
    }

    @BeforeEach
    void stubUsers() {
        lenient().when(userCacheService.usersFor(anyCollection())).thenAnswer(inv -> {
            Collection<Long> ids = inv.getArgument(0);
            return ids.stream().distinct().collect(java.util.stream.Collectors.toMap(
                    java.util.function.Function.identity(),
                    id -> {
                        BaseUserResponse u = new BaseUserResponse();
                        u.setId(id);
                        u.setFirstName("User");
                        u.setLastName("#" + id);
                        return u;
                    }));
        });
        lenient().when(commentRepository.countByActivityIdIn(anyCollection())).thenReturn(List.of());
        lenient().when(likeRepository.countByActivityIdIn(anyCollection())).thenReturn(List.of());
        lenient().when(likeRepository.findActivityIdsLikedBy(anyLong(), anyCollection())).thenReturn(List.of());
    }

    @Nested
    @DisplayName("getFeed")
    class GetFeed {

        @Test
        @DisplayName("hydrates from the internal batch API preserving read-model order and merging social state")
        void hydratesInOrder() {
            Pageable page = PageRequest.of(0, 10);
            when(feedEntryRepository.findByRecipientUserIdOrderByActivityCreatedAtDescIdDesc(VIEWER, page))
                    .thenReturn(new PageImpl<>(List.of(entry(6L, 1L, NOW), entry(12L, 2L, NOW.minusHours(1))), page, 2));
            when(feedItemRepository.findAllByActivityIdIn(List.of(6L, 12L)))
                    .thenReturn(List.of(snapshot(6L, 1L, "Park Run"), snapshot(12L, 2L, "Morning Speed Work")));
            when(activityBatchClient.activitiesById(List.of(6L, 12L)))
                    .thenReturn(Optional.of(Map.of(6L, dto(6L, 1L, "Park Run"), 12L, dto(12L, 2L, "Morning Speed Work"))));
            when(likeRepository.countByActivityIdIn(List.of(6L, 12L)))
                    .thenReturn(List.<Object[]>of(new Object[]{6L, 2L}));
            when(commentRepository.countByActivityIdIn(List.of(6L, 12L)))
                    .thenReturn(List.of(new Object[]{6L, 1L}, new Object[]{12L, 3L}));
            when(likeRepository.findActivityIdsLikedBy(eq(VIEWER), any())).thenReturn(List.of(6L));

            HeaderResponse<ActivityResponse> response = service.getFeed(VIEWER, page);

            assertThat(response.getItems()).extracting(ActivityResponse::getId).containsExactly(6L, 12L);
            ActivityResponse first = response.getItems().get(0);
            assertThat(first.getTitle()).isEqualTo("Park Run");
            assertThat(first.getDescription()).isEqualTo("desc");
            assertThat(first.getActivityType()).isEqualTo(ActivityType.RUN);
            assertThat(first.getLikesCount()).isEqualTo(2);
            assertThat(first.getCommentsCount()).isEqualTo(1);
            assertThat(first.isLikedByCurrentUser()).isTrue();
            assertThat(first.getUser().getId()).isEqualTo(1L);
            assertThat(first.getPhotos()).containsExactly("p.jpg");
            assertThat(response.getItems().get(1).getLikesCount()).isZero();
            assertThat(response.getItems().get(1).getCommentsCount()).isEqualTo(3);
            assertThat(response.getItems().get(1).isLikedByCurrentUser()).isFalse();
            assertThat(response.getHeaders().getFirst("page-total-count")).isEqualTo("1");
            assertThat(response.getHeaders().getFirst("page-total-elements")).isEqualTo("2");
        }

        @Test
        @DisplayName("drops rows whose id activity-service no longer knows (deleted upstream, projection lag)")
        void dropsMissingIdsWhenCallSucceeds() {
            Pageable page = PageRequest.of(0, 10);
            when(feedEntryRepository.findByRecipientUserIdOrderByActivityCreatedAtDescIdDesc(VIEWER, page))
                    .thenReturn(new PageImpl<>(List.of(entry(6L, 1L, NOW), entry(99L, 2L, NOW.minusHours(1))), page, 2));
            when(feedItemRepository.findAllByActivityIdIn(List.of(6L, 99L)))
                    .thenReturn(List.of(snapshot(6L, 1L, "Park Run"), snapshot(99L, 2L, "Ghost")));
            when(activityBatchClient.activitiesById(List.of(6L, 99L)))
                    .thenReturn(Optional.of(Map.of(6L, dto(6L, 1L, "Park Run"))));

            HeaderResponse<ActivityResponse> response = service.getFeed(VIEWER, page);

            assertThat(response.getItems()).extracting(ActivityResponse::getId).containsExactly(6L);
        }

        @Test
        @DisplayName("degrades to the feed_items snapshot when activity-service is down (circuit open)")
        void degradesToSnapshotOnOutage() {
            Pageable page = PageRequest.of(0, 10);
            when(feedEntryRepository.findByRecipientUserIdOrderByActivityCreatedAtDescIdDesc(VIEWER, page))
                    .thenReturn(new PageImpl<>(List.of(entry(6L, 1L, NOW)), page, 1));
            when(feedItemRepository.findAllByActivityIdIn(List.of(6L)))
                    .thenReturn(List.of(snapshot(6L, 1L, "Park Run")));
            when(activityBatchClient.activitiesById(List.of(6L))).thenReturn(Optional.empty());

            HeaderResponse<ActivityResponse> response = service.getFeed(VIEWER, page);

            assertThat(response.getItems()).hasSize(1);
            ActivityResponse row = response.getItems().get(0);
            assertThat(row.getId()).isEqualTo(6L);
            assertThat(row.getTitle()).isEqualTo("Park Run");
            assertThat(row.getActivityType()).isEqualTo(ActivityType.RUN);
            assertThat(row.getDistance()).isEqualTo(8.5);   // meters -> km
            assertThat(row.getDuration()).isEqualTo(2880);
            assertThat(row.getElevationGain()).isEqualTo(156);
            assertThat(row.getUser().getId()).isEqualTo(1L);
            // detail-only fields are the accepted degradation
            assertThat(row.getDescription()).isNull();
            assertThat(row.getRoutePolyline()).isNull();
            assertThat(row.getPhotos()).isNull();
        }

        @Test
        @DisplayName("empty feed short-circuits without calling activity-service")
        void emptyFeedShortCircuits() {
            Pageable page = PageRequest.of(0, 10);
            when(feedEntryRepository.findByRecipientUserIdOrderByActivityCreatedAtDescIdDesc(VIEWER, page))
                    .thenReturn(new PageImpl<>(List.of(), page, 0));

            HeaderResponse<ActivityResponse> response = service.getFeed(VIEWER, page);

            assertThat(response.getItems()).isEmpty();
            assertThat(response.getHeaders().getFirst("page-total-elements")).isEqualTo("0");
            org.mockito.Mockito.verifyNoInteractions(activityBatchClient);
        }
    }

    @Nested
    @DisplayName("getPublicActivities")
    class GetPublic {

        @Test
        @DisplayName("serves non-private snapshots newest-start-first and merges social state")
        void servesPublicSnapshots() {
            Pageable page = PageRequest.of(0, 10);
            when(feedItemRepository.findByIsPrivateFalseOrderByStartedAtDesc(page))
                    .thenReturn(new PageImpl<>(List.of(snapshot(24L, 4L, "Serpentine Swim")), page, 30));
            when(feedItemRepository.findAllByActivityIdIn(List.of(24L)))
                    .thenReturn(List.of(snapshot(24L, 4L, "Serpentine Swim")));
            when(activityBatchClient.activitiesById(List.of(24L)))
                    .thenReturn(Optional.of(Map.of(24L, dto(24L, 4L, "Serpentine Swim"))));

            HeaderResponse<ActivityResponse> response = service.getPublicActivities(VIEWER, page);

            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getUser().getId()).isEqualTo(4L);
            assertThat(response.getHeaders().getFirst("page-total-count")).isEqualTo("3");
            assertThat(response.getHeaders().getFirst("page-total-elements")).isEqualTo("30");
        }
    }
}
