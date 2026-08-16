package com.strava.socialservice.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * H2 (MODE=PostgreSQL) against the real Flyway migrations — verifies both the
 * V2 parity schema + V3 seed dataset and the non-trivial queries the read
 * paths rely on.
 */
@DataJpaTest
@DisplayName("Social repositories on the migrated schema")
class SocialRepositoriesTest {

    @Autowired private FeedItemRepository feedItems;
    @Autowired private UserCacheRepository users;
    @Autowired private FollowEdgeRepository followEdges;
    @Autowired private UserFeedEntryRepository feedEntries;
    @Autowired private CommentRepository comments;
    @Autowired private CommentReactionRepository commentReactions;
    @Autowired private ActivityLikeRepository likes;

    @Nested
    @DisplayName("V3 seed dataset")
    class Seeds {

        @Test
        @DisplayName("mirrors the monolith sample data: 4 users, 24 activities, 53 likes, 15 comments, 9 follow edges")
        void seedCounts() {
            assertThat(users.count()).isEqualTo(4);
            assertThat(feedItems.count()).isEqualTo(24);
            assertThat(likes.count()).isEqualTo(53);
            assertThat(comments.count()).isEqualTo(15);
            assertThat(followEdges.count()).isEqualTo(9);
        }

        @Test
        @DisplayName("user_feed_entries backfill = 24 own rows + 52 follower fan-out rows")
        void feedBackfill() {
            assertThat(feedEntries.count()).isEqualTo(76);
            // user 4 follows nobody -> only their own 4 activities
            Page<UserFeedEntry> adminFeed = feedEntries
                    .findByRecipientUserIdOrderByActivityCreatedAtDescIdDesc(4L, PageRequest.of(0, 50));
            assertThat(adminFeed.getTotalElements()).isEqualTo(4);
            assertThat(adminFeed.getContent()).allSatisfy(e -> assertThat(e.getActorUserId()).isEqualTo(4L));
            // user 1 follows everyone -> sees all 24 activities
            assertThat(feedEntries.findByRecipientUserIdOrderByActivityCreatedAtDescIdDesc(
                    1L, PageRequest.of(0, 50)).getTotalElements()).isEqualTo(24);
        }

        @Test
        @DisplayName("seeded user_cache carries split names from main's sample users")
        void seededNames() {
            UserCache pan = users.findById(1L).orElseThrow();
            assertThat(pan.getFirstName()).isEqualTo("Pan");
            assertThat(pan.getLastName()).isEqualTo("Szczesniak");
        }

        @Test
        @DisplayName("seeded snapshot rows carry title + privacy for the public feed and fallback")
        void seededSnapshots() {
            FeedItem parkRun = feedItems.findByActivityId(6L).orElseThrow();
            assertThat(parkRun.getTitle()).isEqualTo("Park Run");
            assertThat(parkRun.isPrivate()).isFalse();
            assertThat(parkRun.getUserId()).isEqualTo(1L);
            assertThat(parkRun.getDistanceMeters()).isEqualTo(5000.0);
        }
    }

    @Nested
    @DisplayName("feed queries")
    class FeedQueries {

        @Test
        @DisplayName("feed pages order by activity_created_at DESC, id DESC")
        void feedOrdering() {
            Page<UserFeedEntry> page = feedEntries
                    .findByRecipientUserIdOrderByActivityCreatedAtDescIdDesc(1L, PageRequest.of(0, 10));
            assertThat(page.getContent()).hasSize(10);
            List<UserFeedEntry> rows = page.getContent();
            for (int i = 1; i < rows.size(); i++) {
                UserFeedEntry prev = rows.get(i - 1);
                UserFeedEntry curr = rows.get(i);
                boolean ordered = prev.getActivityCreatedAt().isAfter(curr.getActivityCreatedAt())
                        || (prev.getActivityCreatedAt().equals(curr.getActivityCreatedAt())
                            && prev.getId() > curr.getId());
                assertThat(ordered).as("row %d ordered before row %d", i - 1, i).isTrue();
            }
            assertThat(page.getTotalElements()).isEqualTo(24);
            assertThat(page.getTotalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("public query returns only non-private snapshots, newest start first")
        void publicQuery() {
            FeedItem privateItem = feedItems.findByActivityId(24L).orElseThrow();
            privateItem.setPrivate(true);
            feedItems.save(privateItem);

            Page<FeedItem> page = feedItems.findByIsPrivateFalseOrderByStartedAtDesc(PageRequest.of(0, 50));
            assertThat(page.getTotalElements()).isEqualTo(23);
            assertThat(page.getContent()).noneMatch(FeedItem::isPrivate);
            List<FeedItem> rows = page.getContent();
            for (int i = 1; i < rows.size(); i++) {
                assertThat(rows.get(i - 1).getStartedAt())
                        .isAfterOrEqualTo(rows.get(i).getStartedAt());
            }
        }

        @Test
        @DisplayName("findRecentPublicByUserId excludes private snapshots and respects the limit")
        void recentPublic() {
            FeedItem privateItem = feedItems.findByActivityId(6L).orElseThrow();
            privateItem.setPrivate(true);
            feedItems.save(privateItem);

            List<FeedItem> recent = feedItems.findRecentPublicByUserId(1L, PageRequest.of(0, 3));
            assertThat(recent).hasSize(3);
            assertThat(recent).noneMatch(f -> f.getActivityId().equals(6L));
        }
    }

    @Nested
    @DisplayName("count queries (replacing the dropped denormalized counters)")
    class Counts {

        @Test
        @DisplayName("grouped like/comment counts match the seed matrix")
        void groupedCounts() {
            Map<Long, Long> likeCounts = toMap(likes.countByActivityIdIn(List.of(1L, 2L, 4L)));
            assertThat(likeCounts).containsEntry(1L, 3L).containsEntry(2L, 2L).containsEntry(4L, 2L);

            Map<Long, Long> commentCounts = toMap(comments.countByActivityIdIn(List.of(1L, 13L, 4L)));
            assertThat(commentCounts).containsEntry(1L, 2L).containsEntry(13L, 2L);
            assertThat(commentCounts).doesNotContainKey(4L); // no comments on activity 4

            assertThat(likes.countByActivityId(1L)).isEqualTo(3);
            assertThat(comments.countByActivityId(1L)).isEqualTo(2);
        }

        @Test
        @DisplayName("likedBy lookup answers per-viewer state in one query")
        void likedByViewer() {
            // seed: user 2 liked activities 1..6 (among others), user 1 never likes their own
            assertThat(likes.findActivityIdsLikedBy(2L, List.of(1L, 2L, 7L)))
                    .containsExactlyInAnyOrder(1L, 2L);
            assertThat(likes.existsByActivityIdAndUserId(1L, 2L)).isTrue();
            assertThat(likes.existsByActivityIdAndUserId(1L, 1L)).isFalse();
        }
    }

    @Nested
    @DisplayName("comment reactions")
    class Reactions {

        @Test
        @DisplayName("unique (comment,user,emoji) with grouped counts and per-user lookup")
        void reactionQueries() {
            Long commentId = comments.findByActivityIdOrderByCreatedAtDesc(1L).get(0).getId();
            commentReactions.save(CommentReaction.builder().commentId(commentId).userId(1L).emoji("🔥").build());
            commentReactions.save(CommentReaction.builder().commentId(commentId).userId(2L).emoji("🔥").build());
            commentReactions.save(CommentReaction.builder().commentId(commentId).userId(1L).emoji("👍").build());

            Map<String, Long> counts = new HashMap<>();
            for (Object[] row : commentReactions.countReactionsByCommentId(commentId)) {
                counts.put((String) row[0], (Long) row[1]);
            }
            assertThat(counts).containsExactlyInAnyOrderEntriesOf(Map.of("🔥", 2L, "👍", 1L));
            assertThat(commentReactions.findUserReactionsByCommentId(commentId, 1L))
                    .containsExactlyInAnyOrder("🔥", "👍");
            assertThat(commentReactions.findByCommentIdAndUserIdAndEmoji(commentId, 1L, "🔥")).isPresent();
            assertThat(commentReactions.findByCommentIdAndUserIdAndEmoji(commentId, 3L, "🔥")).isEmpty();
        }
    }

    @Nested
    @DisplayName("projector deletes")
    class ProjectorDeletes {

        @Test
        @DisplayName("visibility retraction keeps only the author's row; unfollow purges inherited rows")
        void deleteHelpers() {
            // activity 6 belongs to user 1; recipients seeded: 1 (author), 2, 3
            assertThat(feedEntries.findRecipientIdsByActivityId(6L)).containsExactlyInAnyOrder(1L, 2L, 3L);

            int removed = feedEntries.deleteByActivityIdExceptRecipient(6L, 1L);
            assertThat(removed).isEqualTo(2);
            assertThat(feedEntries.findRecipientIdsByActivityId(6L)).containsExactly(1L);

            // user 2 inherited 6 rows from user 1 (activities 1-5 now that 6 was retracted... still counts rows)
            int purged = feedEntries.deleteByRecipientUserIdAndActorUserId(2L, 1L);
            assertThat(purged).isEqualTo(5);
            assertThat(feedEntries.findActivityIdsByRecipientUserIdAndActorUserId(2L, 1L)).isEmpty();

            int gone = feedEntries.deleteByActivityId(1L);
            assertThat(gone).isEqualTo(2); // author + user 3 (user 2's copy was purged above)
        }
    }

    @Nested
    @DisplayName("follow edges")
    class Follows {

        @Test
        @DisplayName("follower lookup and directional deletes")
        void followEdgeQueries() {
            assertThat(followEdges.findFollowerIdsByFolloweeId(1L)).containsExactlyInAnyOrder(2L, 3L);
            assertThat(followEdges.findFollowerIdsByFolloweeId(4L)).containsExactlyInAnyOrder(1L, 2L, 3L);
            assertThat(followEdges.existsByFollowerIdAndFolloweeId(2L, 1L)).isTrue();
            assertThat(followEdges.existsByFollowerIdAndFolloweeId(4L, 1L)).isFalse();

            assertThat(followEdges.deleteByFollowerIdAndFolloweeId(2L, 1L)).isEqualTo(1);
            assertThat(followEdges.deleteAllInvolving(3L)).isEqualTo(5); // 3 follows 1,2,4 + is followed by 1,2
        }
    }

    private static Map<Long, Long> toMap(List<Object[]> grouped) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : grouped) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }
}
