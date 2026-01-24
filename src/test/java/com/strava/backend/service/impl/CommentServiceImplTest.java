package com.strava.backend.service.impl;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.Comment;
import com.strava.backend.domain.User;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.repository.CommentRepository;
import com.strava.backend.repository.UserRepository;
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
    private ActivityRepository activityRepository;

    @Mock
    private UserRepository userRepository;

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
}
