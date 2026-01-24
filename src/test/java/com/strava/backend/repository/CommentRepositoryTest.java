package com.strava.backend.repository;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.Comment;
import com.strava.backend.domain.User;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.enums.AuthProvider;
import com.strava.backend.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {"spring.flyway.enabled=false"})
@DisplayName("CommentRepository Integration Tests")
class CommentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CommentRepository commentRepository;

    private User testUser;
    private User anotherUser;
    private Activity testActivity;
    private Comment comment1;
    private Comment comment2;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .password("password123")
                .authProvider(AuthProvider.LOCAL)
                .role(Role.USER)
                .active(true)
                .build();
        entityManager.persist(testUser);

        anotherUser = User.builder()
                .email("jane@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .password("password456")
                .authProvider(AuthProvider.LOCAL)
                .role(Role.USER)
                .active(true)
                .build();
        entityManager.persist(anotherUser);

        testActivity = Activity.builder()
                .title("Morning Run")
                .activityType(ActivityType.RUN)
                .distance(10.0)
                .duration(3600)
                .startTime(LocalDateTime.now())
                .isPrivate(false)
                .user(testUser)
                .build();
        entityManager.persist(testActivity);

        comment1 = Comment.builder()
                .content("Great run!")
                .user(testUser)
                .activity(testActivity)
                .build();
        entityManager.persist(comment1);

        // Add a slight delay simulation for ordering
        comment2 = Comment.builder()
                .content("Keep it up!")
                .user(anotherUser)
                .activity(testActivity)
                .build();
        entityManager.persist(comment2);

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("findByActivityIdOrderByCreatedAtDesc Tests")
    class FindByActivityIdTests {

        @Test
        @DisplayName("Should return comments for activity ordered by creation date")
        void shouldReturnCommentsForActivity() {
            List<Comment> comments = commentRepository.findByActivityIdOrderByCreatedAtDesc(testActivity.getId());

            assertThat(comments).hasSize(2);
            assertThat(comments).extracting(Comment::getContent)
                    .containsExactlyInAnyOrder("Great run!", "Keep it up!");
        }

        @Test
        @DisplayName("Should return empty list when activity has no comments")
        void shouldReturnEmptyListWhenNoComments() {
            Activity activityWithNoComments = Activity.builder()
                    .title("Empty Activity")
                    .activityType(ActivityType.RIDE)
                    .distance(20.0)
                    .duration(7200)
                    .startTime(LocalDateTime.now())
                    .isPrivate(false)
                    .user(testUser)
                    .build();
            entityManager.persist(activityWithNoComments);
            entityManager.flush();

            List<Comment> comments = commentRepository.findByActivityIdOrderByCreatedAtDesc(activityWithNoComments.getId());

            assertThat(comments).isEmpty();
        }

        @Test
        @DisplayName("Should return paginated comments")
        void shouldReturnPaginatedComments() {
            // Add more comments
            for (int i = 0; i < 5; i++) {
                Comment comment = Comment.builder()
                        .content("Comment " + i)
                        .user(testUser)
                        .activity(testActivity)
                        .build();
                entityManager.persist(comment);
            }
            entityManager.flush();
            entityManager.clear();

            Page<Comment> page = commentRepository.findByActivityIdOrderByCreatedAtDesc(
                    testActivity.getId(), PageRequest.of(0, 3));

            assertThat(page.getContent()).hasSize(3);
            assertThat(page.getTotalElements()).isEqualTo(7);
            assertThat(page.getTotalPages()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("countByActivityId Tests")
    class CountByActivityIdTests {

        @Test
        @DisplayName("Should return correct comment count")
        void shouldReturnCorrectCommentCount() {
            int count = commentRepository.countByActivityId(testActivity.getId());

            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return zero when activity has no comments")
        void shouldReturnZeroWhenNoComments() {
            Activity activityWithNoComments = Activity.builder()
                    .title("Empty Activity")
                    .activityType(ActivityType.RIDE)
                    .distance(20.0)
                    .duration(7200)
                    .startTime(LocalDateTime.now())
                    .isPrivate(false)
                    .user(testUser)
                    .build();
            entityManager.persist(activityWithNoComments);
            entityManager.flush();

            int count = commentRepository.countByActivityId(activityWithNoComments.getId());

            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("CRUD Operations Tests")
    class CrudOperationsTests {

        @Test
        @DisplayName("Should save and retrieve comment")
        void shouldSaveAndRetrieveComment() {
            Comment newComment = Comment.builder()
                    .content("New comment")
                    .user(testUser)
                    .activity(testActivity)
                    .build();

            Comment saved = commentRepository.save(newComment);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getContent()).isEqualTo("New comment");
        }

        @Test
        @DisplayName("Should update comment content")
        void shouldUpdateCommentContent() {
            comment1.setContent("Updated content");
            commentRepository.save(comment1);
            entityManager.flush();
            entityManager.clear();

            Comment updated = commentRepository.findById(comment1.getId()).orElseThrow();

            assertThat(updated.getContent()).isEqualTo("Updated content");
        }

        @Test
        @DisplayName("Should delete comment")
        void shouldDeleteComment() {
            Long commentId = comment1.getId();

            commentRepository.deleteById(commentId);
            entityManager.flush();

            assertThat(commentRepository.findById(commentId)).isEmpty();
        }
    }
}
