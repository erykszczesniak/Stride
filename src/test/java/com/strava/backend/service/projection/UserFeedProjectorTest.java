package com.strava.backend.service.projection;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.UserFeedEntry;
import com.strava.backend.event.ActivityCreatedEvent;
import com.strava.backend.event.ActivityDeletedEvent;
import com.strava.backend.event.ActivityVisibilityChangedEvent;
import com.strava.backend.event.UserFollowedEvent;
import com.strava.backend.event.UserUnfollowedEvent;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.repository.UserFeedEntryRepository;
import com.strava.backend.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserFeedProjector")
class UserFeedProjectorTest {

    @Mock private UserFeedEntryRepository feedRepository;
    @Mock private UserRepository userRepository;
    @Mock private ActivityRepository activityRepository;

    @InjectMocks private UserFeedProjector projector;

    private static final Long ACTOR_ID = 1L;
    private static final Long ACTIVITY_ID = 100L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 17, 12, 0);

    @Nested
    @DisplayName("onActivityCreated")
    class OnActivityCreated {

        @Test
        @DisplayName("public activity fans out to all followers plus the author")
        void publicActivityFansOutToFollowersAndAuthor() {
            when(userRepository.findFollowerIds(ACTOR_ID)).thenReturn(List.of(2L, 3L, 4L));
            when(feedRepository.findRecipientIdsByActivityId(ACTIVITY_ID)).thenReturn(List.of());

            projector.onActivityCreated(new ActivityCreatedEvent(ACTIVITY_ID, ACTOR_ID, false, NOW));

            ArgumentCaptor<List<UserFeedEntry>> captor = captorForList();
            verify(feedRepository).saveAll(captor.capture());

            Set<Long> recipients = captor.getValue().stream()
                    .map(UserFeedEntry::getRecipientUserId)
                    .collect(Collectors.toSet());
            assertThat(recipients).containsExactlyInAnyOrder(ACTOR_ID, 2L, 3L, 4L);
            assertThat(captor.getValue()).allSatisfy(e -> {
                assertThat(e.getActivityId()).isEqualTo(ACTIVITY_ID);
                assertThat(e.getActorUserId()).isEqualTo(ACTOR_ID);
                assertThat(e.getActivityCreatedAt()).isEqualTo(NOW);
            });
        }

        @Test
        @DisplayName("private activity is materialized only into the author's own feed")
        void privateActivityMaterializesOnlyForAuthor() {
            when(feedRepository.findRecipientIdsByActivityId(ACTIVITY_ID)).thenReturn(List.of());

            projector.onActivityCreated(new ActivityCreatedEvent(ACTIVITY_ID, ACTOR_ID, true, NOW));

            ArgumentCaptor<List<UserFeedEntry>> captor = captorForList();
            verify(feedRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(1);
            assertThat(captor.getValue().get(0).getRecipientUserId()).isEqualTo(ACTOR_ID);
            verify(userRepository, never()).findFollowerIds(any());
        }

        @Test
        @DisplayName("re-firing the event is idempotent — already-materialized recipients are skipped")
        void reFiringIsIdempotent() {
            when(userRepository.findFollowerIds(ACTOR_ID)).thenReturn(List.of(2L, 3L));
            // Author + follower 2 already have rows; only follower 3 is missing.
            when(feedRepository.findRecipientIdsByActivityId(ACTIVITY_ID))
                    .thenReturn(List.of(ACTOR_ID, 2L));

            projector.onActivityCreated(new ActivityCreatedEvent(ACTIVITY_ID, ACTOR_ID, false, NOW));

            ArgumentCaptor<List<UserFeedEntry>> captor = captorForList();
            verify(feedRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(1);
            assertThat(captor.getValue().get(0).getRecipientUserId()).isEqualTo(3L);
        }

        @Test
        @DisplayName("nothing to insert short-circuits without a saveAll call")
        void nothingToInsertSkipsSave() {
            when(userRepository.findFollowerIds(ACTOR_ID)).thenReturn(List.of(2L));
            when(feedRepository.findRecipientIdsByActivityId(ACTIVITY_ID))
                    .thenReturn(List.of(ACTOR_ID, 2L));

            projector.onActivityCreated(new ActivityCreatedEvent(ACTIVITY_ID, ACTOR_ID, false, NOW));

            verify(feedRepository, never()).saveAll(any());
        }
    }

    @Nested
    @DisplayName("onActivityDeleted")
    class OnActivityDeleted {

        @Test
        @DisplayName("removes every feed row for the activity")
        void removesAllRows() {
            when(feedRepository.deleteByActivityId(ACTIVITY_ID)).thenReturn(5);

            projector.onActivityDeleted(new ActivityDeletedEvent(ACTIVITY_ID));

            verify(feedRepository).deleteByActivityId(ACTIVITY_ID);
        }
    }

    @Nested
    @DisplayName("onActivityVisibilityChanged")
    class OnVisibilityChanged {

        @Test
        @DisplayName("public to private retracts the activity from all follower feeds but keeps the author's")
        void publicToPrivateRetractsFromFollowers() {
            when(feedRepository.deleteByActivityIdExceptRecipient(ACTIVITY_ID, ACTOR_ID)).thenReturn(3);

            projector.onActivityVisibilityChanged(
                    new ActivityVisibilityChangedEvent(ACTIVITY_ID, ACTOR_ID, true, NOW));

            verify(feedRepository).deleteByActivityIdExceptRecipient(ACTIVITY_ID, ACTOR_ID);
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("private to public re-fans the activity to current followers")
        void privateToPublicReFansOut() {
            when(userRepository.findFollowerIds(ACTOR_ID)).thenReturn(List.of(2L, 3L));
            when(feedRepository.findRecipientIdsByActivityId(ACTIVITY_ID))
                    .thenReturn(List.of(ACTOR_ID)); // only author's row exists from when it was private

            projector.onActivityVisibilityChanged(
                    new ActivityVisibilityChangedEvent(ACTIVITY_ID, ACTOR_ID, false, NOW));

            ArgumentCaptor<List<UserFeedEntry>> captor = captorForList();
            verify(feedRepository).saveAll(captor.capture());
            Set<Long> recipients = captor.getValue().stream()
                    .map(UserFeedEntry::getRecipientUserId)
                    .collect(Collectors.toSet());
            assertThat(recipients).containsExactlyInAnyOrder(2L, 3L);
        }
    }

    @Nested
    @DisplayName("onUserFollowed")
    class OnUserFollowed {

        @Test
        @DisplayName("backfills the follower's feed with the followee's recent public activities")
        void backfillsRecentActivities() {
            Long followerId = 2L;
            Long followeeId = ACTOR_ID;

            Activity a1 = activity(101L, NOW.minusDays(1));
            Activity a2 = activity(102L, NOW.minusDays(2));
            when(activityRepository.findRecentPublicByUserId(eq(followeeId), any(Pageable.class)))
                    .thenReturn(List.of(a1, a2));
            when(feedRepository.findActivityIdsByRecipientUserIdAndActorUserId(followerId, followeeId))
                    .thenReturn(List.of());

            projector.onUserFollowed(new UserFollowedEvent(followerId, followeeId));

            ArgumentCaptor<List<UserFeedEntry>> captor = captorForList();
            verify(feedRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(2);
            assertThat(captor.getValue()).allSatisfy(e -> {
                assertThat(e.getRecipientUserId()).isEqualTo(followerId);
                assertThat(e.getActorUserId()).isEqualTo(followeeId);
            });
        }

        @Test
        @DisplayName("skips activities the follower already has materialized")
        void skipsAlreadyMaterialized() {
            Long followerId = 2L;
            Long followeeId = ACTOR_ID;

            Activity a1 = activity(101L, NOW.minusDays(1));
            Activity a2 = activity(102L, NOW.minusDays(2));
            when(activityRepository.findRecentPublicByUserId(eq(followeeId), any(Pageable.class)))
                    .thenReturn(List.of(a1, a2));
            when(feedRepository.findActivityIdsByRecipientUserIdAndActorUserId(followerId, followeeId))
                    .thenReturn(List.of(101L)); // a1 already present

            projector.onUserFollowed(new UserFollowedEvent(followerId, followeeId));

            ArgumentCaptor<List<UserFeedEntry>> captor = captorForList();
            verify(feedRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(1);
            assertThat(captor.getValue().get(0).getActivityId()).isEqualTo(102L);
        }

        @Test
        @DisplayName("no activities to backfill skips saveAll entirely")
        void noActivitiesShortCircuits() {
            when(activityRepository.findRecentPublicByUserId(eq(ACTOR_ID), any(Pageable.class)))
                    .thenReturn(List.of());

            projector.onUserFollowed(new UserFollowedEvent(2L, ACTOR_ID));

            verify(feedRepository, never()).saveAll(any());
        }
    }

    @Nested
    @DisplayName("onUserUnfollowed")
    class OnUserUnfollowed {

        @Test
        @DisplayName("purges every feed row where follower received an activity from the unfollowed user")
        void purgesFromTo() {
            projector.onUserUnfollowed(new UserUnfollowedEvent(2L, ACTOR_ID));

            verify(feedRepository).deleteByRecipientUserIdAndActorUserId(2L, ACTOR_ID);
        }
    }

    private static Activity activity(Long id, LocalDateTime createdAt) {
        Activity a = new Activity();
        a.setId(id);
        a.setCreatedAt(createdAt);
        return a;
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<List<UserFeedEntry>> captorForList() {
        return ArgumentCaptor.forClass(List.class);
    }
}
