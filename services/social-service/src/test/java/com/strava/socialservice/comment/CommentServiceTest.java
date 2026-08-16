package com.strava.socialservice.comment;

import com.strava.socialservice.domain.Comment;
import com.strava.socialservice.domain.CommentReaction;
import com.strava.socialservice.domain.CommentReactionRepository;
import com.strava.socialservice.domain.CommentRepository;
import com.strava.socialservice.domain.FeedItem;
import com.strava.socialservice.domain.FeedItemRepository;
import com.strava.socialservice.event.SocialEventPublisher;
import com.strava.socialservice.exception.ApiRequestException;
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
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.strava.socialservice.constants.ErrorMessage.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService")
class CommentServiceTest {

    @Mock private CommentRepository commentRepository;
    @Mock private CommentReactionRepository commentReactionRepository;
    @Mock private FeedItemRepository feedItemRepository;
    @Mock private UserCacheService userCacheService;
    @Mock private SocialEventPublisher eventPublisher;

    @InjectMocks private CommentService service;

    private static final Long ACTIVITY_ID = 7L;
    private static final Long OWNER_ID = 2L;
    private static final Long AUTHOR_ID = 1L;

    private FeedItem feedItem;

    @BeforeEach
    void setUp() {
        feedItem = FeedItem.builder()
                .activityId(ACTIVITY_ID)
                .userId(OWNER_ID)
                .sport("RUN")
                .build();
        lenient().when(userCacheService.userFor(anyLong())).thenAnswer(inv -> {
            BaseUserResponse u = new BaseUserResponse();
            u.setId(inv.getArgument(0));
            u.setFirstName("Pan");
            u.setLastName("Szczesniak");
            return u;
        });
        lenient().when(userCacheService.displayNameFor(anyLong())).thenReturn("Pan Szczesniak");
    }

    @Nested
    @DisplayName("createComment")
    class CreateComment {

        @Test
        @DisplayName("saves the comment and publishes an enriched CommentAddedEvent with owner + author name")
        void createsAndPublishes() {
            when(feedItemRepository.findByActivityId(ACTIVITY_ID)).thenReturn(Optional.of(feedItem));
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

            CommentResponse response = service.createComment(ACTIVITY_ID, AUTHOR_ID, "Nice run!");

            assertThat(response.getContent()).isEqualTo("Nice run!");
            assertThat(response.getUser().getId()).isEqualTo(AUTHOR_ID);
            assertThat(response.getUser().getFullName()).isEqualTo("Pan Szczesniak");
            verify(eventPublisher).publishCommentAdded(any(Comment.class), eq(OWNER_ID), eq("Pan Szczesniak"));
        }

        @Test
        @DisplayName("404s with main's text when the activity is unknown")
        void unknownActivity404s() {
            when(feedItemRepository.findByActivityId(ACTIVITY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createComment(ACTIVITY_ID, AUTHOR_ID, "hi"))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(ACTIVITY_NOT_FOUND)
                    .extracting(e -> ((ApiRequestException) e).getStatus())
                    .isEqualTo(HttpStatus.NOT_FOUND);
            verify(commentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getCommentsByActivityId")
    class GetComments {

        @Test
        @DisplayName("404s when the activity is unknown, mirrors main's existence check")
        void unknownActivity404s() {
            when(feedItemRepository.findByActivityId(ACTIVITY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCommentsByActivityId(ACTIVITY_ID))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(ACTIVITY_NOT_FOUND);
        }

        @Test
        @DisplayName("embeds the author from user_cache on every comment")
        void embedsAuthors() {
            when(feedItemRepository.findByActivityId(ACTIVITY_ID)).thenReturn(Optional.of(feedItem));
            Comment comment = Comment.builder().id(11L).activityId(ACTIVITY_ID).userId(AUTHOR_ID).content("x").build();
            when(commentRepository.findByActivityIdOrderByCreatedAtDesc(ACTIVITY_ID)).thenReturn(List.of(comment));
            BaseUserResponse author = new BaseUserResponse();
            author.setId(AUTHOR_ID);
            author.setFirstName("Pan");
            author.setLastName("Szczesniak");
            when(userCacheService.usersFor(List.of(AUTHOR_ID))).thenReturn(Map.of(AUTHOR_ID, author));

            List<CommentResponse> comments = service.getCommentsByActivityId(ACTIVITY_ID);

            assertThat(comments).hasSize(1);
            assertThat(comments.get(0).getId()).isEqualTo(11L);
            assertThat(comments.get(0).getUser().getFirstName()).isEqualTo("Pan");
        }
    }

    @Nested
    @DisplayName("updateComment / deleteComment")
    class UpdateDelete {

        private final Comment comment = Comment.builder()
                .id(11L).activityId(ACTIVITY_ID).userId(AUTHOR_ID).content("old").build();

        @Test
        @DisplayName("author can update; content is replaced")
        void authorUpdates() {
            when(commentRepository.findById(11L)).thenReturn(Optional.of(comment));
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

            CommentResponse response = service.updateComment(11L, AUTHOR_ID, "new");

            assertThat(response.getContent()).isEqualTo("new");
        }

        @Test
        @DisplayName("non-author update is denied with main's 403 text")
        void nonAuthorUpdateDenied() {
            when(commentRepository.findById(11L)).thenReturn(Optional.of(comment));

            assertThatThrownBy(() -> service.updateComment(11L, 99L, "new"))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(ACCESS_DENIED)
                    .extracting(e -> ((ApiRequestException) e).getStatus())
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("unknown comment 404s with main's text")
        void unknownComment404s() {
            when(commentRepository.findById(11L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteComment(11L, AUTHOR_ID))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(COMMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("non-author delete is denied; author delete removes the comment")
        void deleteRules() {
            when(commentRepository.findById(11L)).thenReturn(Optional.of(comment));

            assertThatThrownBy(() -> service.deleteComment(11L, 99L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(ACCESS_DENIED);

            service.deleteComment(11L, AUTHOR_ID);
            verify(commentRepository).delete(comment);
        }
    }

    @Nested
    @DisplayName("reactions")
    class Reactions {

        @Test
        @DisplayName("addReaction is idempotent — an existing (comment,user,emoji) row is returned, not duplicated")
        void addIsIdempotent() {
            when(commentRepository.existsById(11L)).thenReturn(true);
            CommentReaction existing = CommentReaction.builder()
                    .id(5L).commentId(11L).userId(AUTHOR_ID).emoji("🔥").build();
            when(commentReactionRepository.findByCommentIdAndUserIdAndEmoji(11L, AUTHOR_ID, "🔥"))
                    .thenReturn(Optional.of(existing));

            CommentReaction result = service.addReaction(11L, AUTHOR_ID, "🔥");

            assertThat(result).isSameAs(existing);
            verify(commentReactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("addReaction on an unknown comment 404s")
        void addOnUnknownComment404s() {
            when(commentRepository.existsById(11L)).thenReturn(false);

            assertThatThrownBy(() -> service.addReaction(11L, AUTHOR_ID, "🔥"))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(COMMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("removeReaction 404s with main's text when the reaction does not exist")
        void removeMissing404s() {
            when(commentReactionRepository.findByCommentIdAndUserIdAndEmoji(11L, AUTHOR_ID, "🔥"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.removeReaction(11L, AUTHOR_ID, "🔥"))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(REACTION_NOT_FOUND);
        }

        @Test
        @DisplayName("getReactionCounts returns main's emoji->count map shape")
        void countsMapShape() {
            when(commentReactionRepository.countReactionsByCommentId(11L))
                    .thenReturn(List.of(new Object[]{"🔥", 2L}, new Object[]{"👍", 1L}));

            Map<String, Long> counts = service.getReactionCounts(11L);

            assertThat(counts).containsExactlyInAnyOrderEntriesOf(Map.of("🔥", 2L, "👍", 1L));
        }
    }
}
