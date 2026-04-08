package com.strava.backend.service.impl;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.Comment;
import com.strava.backend.domain.CommentReaction;
import com.strava.backend.domain.User;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.repository.CommentReactionRepository;
import com.strava.backend.repository.CommentRepository;
import com.strava.backend.repository.UserRepository;
import com.strava.backend.service.NotificationService;
import com.strava.backend.util.TestDataFactory;
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

import static com.strava.backend.constants.ErrorMessage.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentServiceImpl Unit Tests")
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentReactionRepository commentReactionRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CommentServiceImpl commentService;

    private User testUser;
    private User anotherUser;
    private Activity testActivity;
    private Comment testComment;

    @BeforeEach
    void setUp() {
        testUser = TestDataFactory.createUser(1L, "john@example.com", "John", "Doe");
        anotherUser = TestDataFactory.createUser(2L, "jane@example.com", "Jane", "Smith");
        testActivity = TestDataFactory.createActivity(1L, testUser);
        testComment = TestDataFactory.createComment(1L, testUser, testActivity);
    }

    @Nested
    @DisplayName("getCommentsByActivityId Tests")
    class GetCommentsByActivityIdTests {

        @Test
        @DisplayName("Should return comments when activity exists")
        void shouldReturnCommentsWhenActivityExists() {
            // Given
            Comment comment2 = TestDataFactory.createComment(2L, anotherUser, testActivity);
            given(activityRepository.existsById(1L)).willReturn(true);
            given(commentRepository.findByActivityIdOrderByCreatedAtDesc(1L))
                    .willReturn(List.of(testComment, comment2));

            // When
            List<Comment> result = commentService.getCommentsByActivityId(1L);

            // Then
            assertThat(result).hasSize(2);
            then(commentRepository).should().findByActivityIdOrderByCreatedAtDesc(1L);
        }

        @Test
        @DisplayName("Should return empty list when no comments")
        void shouldReturnEmptyListWhenNoComments() {
            // Given
            given(activityRepository.existsById(1L)).willReturn(true);
            given(commentRepository.findByActivityIdOrderByCreatedAtDesc(1L)).willReturn(List.of());

            // When
            List<Comment> result = commentService.getCommentsByActivityId(1L);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when activity not found")
        void shouldThrowExceptionWhenActivityNotFound() {
            // Given
            given(activityRepository.existsById(999L)).willReturn(false);

            // When/Then
            assertThatThrownBy(() -> commentService.getCommentsByActivityId(999L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(ACTIVITY_NOT_FOUND)
                    .extracting("status")
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getCommentById Tests")
    class GetCommentByIdTests {

        @Test
        @DisplayName("Should return comment when found")
        void shouldReturnCommentWhenFound() {
            // Given
            given(commentRepository.findById(1L)).willReturn(Optional.of(testComment));

            // When
            Comment result = commentService.getCommentById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getContent()).isEqualTo("Great activity!");
        }

        @Test
        @DisplayName("Should throw exception when comment not found")
        void shouldThrowExceptionWhenCommentNotFound() {
            // Given
            given(commentRepository.findById(999L)).willReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> commentService.getCommentById(999L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(COMMENT_NOT_FOUND)
                    .extracting("status")
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("createComment Tests")
    class CreateCommentTests {

        @Test
        @DisplayName("Should create comment successfully")
        void shouldCreateCommentSuccessfully() {
            // Given
            String content = "Nice run!";
            given(activityRepository.findById(1L)).willReturn(Optional.of(testActivity));
            given(userRepository.findById(2L)).willReturn(Optional.of(anotherUser));
            given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> {
                Comment saved = invocation.getArgument(0);
                saved.setId(2L);
                return saved;
            });

            // When
            Comment result = commentService.createComment(1L, 2L, content);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEqualTo(content);
            assertThat(result.getActivity()).isEqualTo(testActivity);
            assertThat(result.getUser()).isEqualTo(anotherUser);

            then(commentRepository).should().save(any(Comment.class));
        }

        @Test
        @DisplayName("Should throw exception when activity not found")
        void shouldThrowExceptionWhenActivityNotFound() {
            // Given
            given(activityRepository.findById(999L)).willReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> commentService.createComment(999L, 1L, "Comment"))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(ACTIVITY_NOT_FOUND);

            then(commentRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            given(activityRepository.findById(1L)).willReturn(Optional.of(testActivity));
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> commentService.createComment(1L, 999L, "Comment"))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(USER_NOT_FOUND);

            then(commentRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateComment Tests")
    class UpdateCommentTests {

        @Test
        @DisplayName("Should update comment when user is author")
        void shouldUpdateCommentWhenUserIsAuthor() {
            // Given
            String updatedContent = "Updated comment";
            given(commentRepository.findById(1L)).willReturn(Optional.of(testComment));
            given(commentRepository.save(any(Comment.class))).willReturn(testComment);

            // When
            Comment result = commentService.updateComment(1L, 1L, updatedContent);

            // Then
            assertThat(result.getContent()).isEqualTo(updatedContent);
            then(commentRepository).should().save(testComment);
        }

        @Test
        @DisplayName("Should throw exception when comment not found")
        void shouldThrowExceptionWhenCommentNotFound() {
            // Given
            given(commentRepository.findById(999L)).willReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> commentService.updateComment(999L, 1L, "Updated"))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(COMMENT_NOT_FOUND);

            then(commentRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when user is not author")
        void shouldThrowExceptionWhenUserIsNotAuthor() {
            // Given
            given(commentRepository.findById(1L)).willReturn(Optional.of(testComment));

            // When/Then
            assertThatThrownBy(() -> commentService.updateComment(1L, 2L, "Updated"))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(ACCESS_DENIED)
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);

            then(commentRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteComment Tests")
    class DeleteCommentTests {

        @Test
        @DisplayName("Should delete comment when user is author")
        void shouldDeleteCommentWhenUserIsAuthor() {
            // Given
            given(commentRepository.findById(1L)).willReturn(Optional.of(testComment));

            // When
            commentService.deleteComment(1L, 1L);

            // Then
            then(commentRepository).should().delete(testComment);
        }

        @Test
        @DisplayName("Should throw exception when comment not found")
        void shouldThrowExceptionWhenCommentNotFound() {
            // Given
            given(commentRepository.findById(999L)).willReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> commentService.deleteComment(999L, 1L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(COMMENT_NOT_FOUND);

            then(commentRepository).should(never()).delete(any());
        }

        @Test
        @DisplayName("Should throw exception when user is not author")
        void shouldThrowExceptionWhenUserIsNotAuthor() {
            // Given
            given(commentRepository.findById(1L)).willReturn(Optional.of(testComment));

            // When/Then
            assertThatThrownBy(() -> commentService.deleteComment(1L, 2L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(ACCESS_DENIED)
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);

            then(commentRepository).should(never()).delete(any());
        }
    }

    @Nested
    @DisplayName("addReaction Tests")
    class AddReactionTests {

        @Test
        @DisplayName("Should add reaction successfully")
        void shouldAddReactionSuccessfully() {
            // Given
            given(commentRepository.findById(1L)).willReturn(Optional.of(testComment));
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(commentReactionRepository.findByCommentIdAndUserIdAndEmoji(1L, 1L, "👍"))
                    .willReturn(Optional.empty());
            given(commentReactionRepository.save(any(CommentReaction.class))).willAnswer(invocation -> {
                CommentReaction saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            // When
            CommentReaction result = commentService.addReaction(1L, 1L, "👍");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getEmoji()).isEqualTo("👍");
            then(commentReactionRepository).should().save(any(CommentReaction.class));
        }

        @Test
        @DisplayName("Should return existing reaction if already exists")
        void shouldReturnExistingReactionIfAlreadyExists() {
            // Given
            CommentReaction existingReaction = TestDataFactory.createCommentReaction(1L, testUser, testComment, "👍");
            given(commentRepository.findById(1L)).willReturn(Optional.of(testComment));
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(commentReactionRepository.findByCommentIdAndUserIdAndEmoji(1L, 1L, "👍"))
                    .willReturn(Optional.of(existingReaction));

            // When
            CommentReaction result = commentService.addReaction(1L, 1L, "👍");

            // Then
            assertThat(result).isEqualTo(existingReaction);
            then(commentReactionRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when comment not found")
        void shouldThrowExceptionWhenCommentNotFound() {
            // Given
            given(commentRepository.findById(999L)).willReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> commentService.addReaction(999L, 1L, "👍"))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(COMMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            given(commentRepository.findById(1L)).willReturn(Optional.of(testComment));
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> commentService.addReaction(1L, 999L, "👍"))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("removeReaction Tests")
    class RemoveReactionTests {

        @Test
        @DisplayName("Should remove reaction successfully")
        void shouldRemoveReactionSuccessfully() {
            // Given
            CommentReaction reaction = TestDataFactory.createCommentReaction(1L, testUser, testComment, "👍");
            given(commentReactionRepository.findByCommentIdAndUserIdAndEmoji(1L, 1L, "👍"))
                    .willReturn(Optional.of(reaction));

            // When
            commentService.removeReaction(1L, 1L, "👍");

            // Then
            then(commentReactionRepository).should().delete(reaction);
        }

        @Test
        @DisplayName("Should throw exception when reaction not found")
        void shouldThrowExceptionWhenReactionNotFound() {
            // Given
            given(commentReactionRepository.findByCommentIdAndUserIdAndEmoji(1L, 1L, "👍"))
                    .willReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> commentService.removeReaction(1L, 1L, "👍"))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("Reaction not found");
        }
    }

    @Nested
    @DisplayName("getReactionCounts Tests")
    class GetReactionCountsTests {

        @Test
        @DisplayName("Should return reaction counts")
        void shouldReturnReactionCounts() {
            // Given
            List<Object[]> mockResults = List.of(
                    new Object[]{"👍", 5L},
                    new Object[]{"❤️", 3L}
            );
            given(commentReactionRepository.countReactionsByCommentId(1L)).willReturn(mockResults);

            // When
            Map<String, Long> result = commentService.getReactionCounts(1L);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get("👍")).isEqualTo(5L);
            assertThat(result.get("❤️")).isEqualTo(3L);
        }

        @Test
        @DisplayName("Should return empty map when no reactions")
        void shouldReturnEmptyMapWhenNoReactions() {
            // Given
            given(commentReactionRepository.countReactionsByCommentId(1L)).willReturn(List.of());

            // When
            Map<String, Long> result = commentService.getReactionCounts(1L);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getUserReactions Tests")
    class GetUserReactionsTests {

        @Test
        @DisplayName("Should return user reactions")
        void shouldReturnUserReactions() {
            // Given
            given(commentReactionRepository.findUserReactionsByCommentId(1L, 1L))
                    .willReturn(List.of("👍", "🔥"));

            // When
            List<String> result = commentService.getUserReactions(1L, 1L);

            // Then
            assertThat(result).containsExactly("👍", "🔥");
        }

        @Test
        @DisplayName("Should return empty list when no user reactions")
        void shouldReturnEmptyListWhenNoUserReactions() {
            // Given
            given(commentReactionRepository.findUserReactionsByCommentId(1L, 1L))
                    .willReturn(List.of());

            // When
            List<String> result = commentService.getUserReactions(1L, 1L);

            // Then
            assertThat(result).isEmpty();
        }
    }
}
