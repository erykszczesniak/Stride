package com.strava.socialservice.kafka;

import com.strava.shared.events.ActivityCreatedEvent;
import com.strava.shared.events.ActivityDeletedEvent;
import com.strava.shared.events.ActivityVisibilityChangedEvent;
import com.strava.shared.events.UserDeletedEvent;
import com.strava.shared.events.UserFollowedEvent;
import com.strava.shared.events.UserRegisteredEvent;
import com.strava.shared.events.UserUnfollowedEvent;
import com.strava.socialservice.domain.ActivityLikeRepository;
import com.strava.socialservice.domain.CommentReactionRepository;
import com.strava.socialservice.domain.CommentRepository;
import com.strava.socialservice.domain.FeedItem;
import com.strava.socialservice.domain.FeedItemRepository;
import com.strava.socialservice.domain.FollowEdge;
import com.strava.socialservice.domain.FollowEdgeRepository;
import com.strava.socialservice.domain.UserCache;
import com.strava.socialservice.domain.UserCacheRepository;
import com.strava.socialservice.domain.UserFeedEntryRepository;
import com.strava.socialservice.projection.UserFeedProjector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventConsumers")
class EventConsumersTest {

    @Mock private FeedItemRepository feedItems;
    @Mock private UserCacheRepository users;
    @Mock private FollowEdgeRepository followEdges;
    @Mock private UserFeedEntryRepository feedEntries;
    @Mock private CommentRepository comments;
    @Mock private CommentReactionRepository commentReactions;
    @Mock private ActivityLikeRepository likes;
    @Mock private UserFeedProjector projector;

    @InjectMocks private EventConsumers consumers;

    private static final Instant OCCURRED = Instant.parse("2026-07-01T10:00:00Z");
    private static final LocalDateTime OCCURRED_LOCAL = LocalDateTime.ofInstant(OCCURRED, ZoneOffset.UTC);

    private ActivityCreatedEvent createdEvent(boolean isPrivate) {
        return new ActivityCreatedEvent(6L, 1L, "RUN", "Park Run", isPrivate,
                5000.0, 1500L, 15.0, Instant.parse("2026-07-01T08:00:00Z"), OCCURRED);
    }

    @Nested
    @DisplayName("user cache")
    class Users {

        @Test
        @DisplayName("onUserRegistered inserts a user_cache row with the display name split at the first space")
        void registersUser() {
            consumers.onUserRegistered(new UserRegisteredEvent(1L, "pan@example.com",
                    "Pan Szczesniak", "a.png", OCCURRED));

            ArgumentCaptor<UserCache> captor = ArgumentCaptor.forClass(UserCache.class);
            verify(users).save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(1L);
            assertThat(captor.getValue().getFirstName()).isEqualTo("Pan");
            assertThat(captor.getValue().getLastName()).isEqualTo("Szczesniak");
            assertThat(captor.getValue().getAvatar()).isEqualTo("a.png");
        }

        @Test
        @DisplayName("splitDisplayName handles single-word and null names")
        void splitCases() {
            assertThat(EventConsumers.splitDisplayName("Cher")).containsExactly("Cher", null);
            assertThat(EventConsumers.splitDisplayName(" John  Ronald Doe ")).containsExactly("John", "Ronald Doe");
            assertThat(EventConsumers.splitDisplayName(null)).containsExactly(null, null);
        }

        @Test
        @DisplayName("onUserDeleted purges cache, follow edges, feed rows and the user's social content")
        void deletesUserData() {
            consumers.onUserDeleted(new UserDeletedEvent(1L, OCCURRED));

            verify(users).deleteById(1L);
            verify(followEdges).deleteAllInvolving(1L);
            verify(feedEntries).deleteAllInvolving(1L);
            verify(comments).deleteByUserId(1L);
            verify(commentReactions).deleteByUserId(1L);
            verify(likes).deleteByUserId(1L);
            verify(feedItems).deleteByUserId(1L);
        }
    }

    @Nested
    @DisplayName("activity snapshots + projection")
    class Activities {

        @Test
        @DisplayName("onActivityCreated inserts the snapshot (title + privacy included) and fans out via the projector")
        void createsSnapshotAndFansOut() {
            when(feedItems.findByActivityId(6L)).thenReturn(Optional.empty());

            consumers.onActivityCreated(createdEvent(false));

            ArgumentCaptor<FeedItem> captor = ArgumentCaptor.forClass(FeedItem.class);
            verify(feedItems).save(captor.capture());
            FeedItem item = captor.getValue();
            assertThat(item.getActivityId()).isEqualTo(6L);
            assertThat(item.getUserId()).isEqualTo(1L);
            assertThat(item.getTitle()).isEqualTo("Park Run");
            assertThat(item.isPrivate()).isFalse();
            assertThat(item.getDistanceMeters()).isEqualTo(5000.0);
            assertThat(item.getOccurredAt()).isEqualTo(OCCURRED_LOCAL);

            verify(projector).onActivityCreated(6L, 1L, false, OCCURRED_LOCAL);
        }

        @Test
        @DisplayName("onActivityUpdated refreshes sport/distance/duration/title/privacy on the existing snapshot without re-fan-out")
        void updateRefreshesSnapshot() {
            FeedItem existing = FeedItem.builder()
                    .activityId(6L).userId(1L).sport("RIDE").title("Old title")
                    .occurredAt(OCCURRED_LOCAL.minusDays(1)).startedAt(OCCURRED_LOCAL.minusDays(1))
                    .build();
            when(feedItems.findByActivityId(6L)).thenReturn(Optional.of(existing));

            consumers.onActivityUpdated(createdEvent(true));

            assertThat(existing.getSport()).isEqualTo("RUN");
            assertThat(existing.getTitle()).isEqualTo("Park Run");
            assertThat(existing.isPrivate()).isTrue();
            assertThat(existing.getDistanceMeters()).isEqualTo(5000.0);
            assertThat(existing.getDurationSeconds()).isEqualTo(1500L);
            // creation marker survives the refresh
            assertThat(existing.getOccurredAt()).isEqualTo(OCCURRED_LOCAL.minusDays(1));
            verify(feedItems).save(existing);
            verify(projector, never()).onActivityCreated(any(), any(), org.mockito.ArgumentMatchers.anyBoolean(), any());
        }

        @Test
        @DisplayName("onActivityCreated redelivery upserts the same snapshot row instead of duplicating")
        void createdRedeliveryUpserts() {
            FeedItem existing = FeedItem.builder()
                    .activityId(6L).userId(1L).sport("RUN").occurredAt(OCCURRED_LOCAL).startedAt(OCCURRED_LOCAL)
                    .build();
            when(feedItems.findByActivityId(6L)).thenReturn(Optional.of(existing));

            consumers.onActivityCreated(createdEvent(false));

            verify(feedItems).save(existing);
        }

        @Test
        @DisplayName("onActivityDeleted purges snapshot, feed rows, comments and likes")
        void deletePurges() {
            consumers.onActivityDeleted(new ActivityDeletedEvent(6L, 1L, OCCURRED));

            verify(projector).onActivityDeleted(6L);
            verify(feedItems).deleteByActivityId(6L);
            verify(comments).deleteByActivityId(6L);
            verify(likes).deleteByActivityId(6L);
        }

        @Test
        @DisplayName("onActivityVisibilityChanged flips the snapshot flag and delegates with the event's creation timestamp")
        void visibilityChange() {
            FeedItem existing = FeedItem.builder()
                    .activityId(6L).userId(1L).sport("RUN").occurredAt(OCCURRED_LOCAL).startedAt(OCCURRED_LOCAL)
                    .build();
            when(feedItems.findByActivityId(6L)).thenReturn(Optional.of(existing));
            Instant createdAt = Instant.parse("2026-06-30T09:00:00Z");

            consumers.onActivityVisibilityChanged(
                    new ActivityVisibilityChangedEvent(6L, 1L, true, createdAt, OCCURRED));

            assertThat(existing.isPrivate()).isTrue();
            verify(projector).onActivityVisibilityChanged(6L, 1L, true,
                    LocalDateTime.ofInstant(createdAt, ZoneOffset.UTC));
        }
    }

    @Nested
    @DisplayName("follow graph")
    class Follows {

        @Test
        @DisplayName("onUserFollowed stores the edge once and triggers the projector backfill")
        void followInsertsEdge() {
            when(followEdges.existsByFollowerIdAndFolloweeId(2L, 1L)).thenReturn(false);

            consumers.onUserFollowed(new UserFollowedEvent(2L, 1L, "John Doe", OCCURRED));

            ArgumentCaptor<FollowEdge> captor = ArgumentCaptor.forClass(FollowEdge.class);
            verify(followEdges).save(captor.capture());
            assertThat(captor.getValue().getFollowerId()).isEqualTo(2L);
            assertThat(captor.getValue().getFolloweeId()).isEqualTo(1L);
            verify(projector).onUserFollowed(2L, 1L);
        }

        @Test
        @DisplayName("onUserFollowed redelivery does not duplicate the edge but still re-runs the idempotent backfill")
        void followRedeliveryIsIdempotent() {
            when(followEdges.existsByFollowerIdAndFolloweeId(2L, 1L)).thenReturn(true);

            consumers.onUserFollowed(new UserFollowedEvent(2L, 1L, "John Doe", OCCURRED));

            verify(followEdges, never()).save(any());
            verify(projector).onUserFollowed(2L, 1L);
        }

        @Test
        @DisplayName("onUserUnfollowed removes the edge and purges the inherited feed rows")
        void unfollowRemovesEdge() {
            consumers.onUserUnfollowed(new UserUnfollowedEvent(2L, 1L, OCCURRED));

            verify(followEdges).deleteByFollowerIdAndFolloweeId(eq(2L), eq(1L));
            verify(projector).onUserUnfollowed(2L, 1L);
        }
    }
}
