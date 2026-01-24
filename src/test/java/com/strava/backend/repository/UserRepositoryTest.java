package com.strava.backend.repository;

import com.strava.backend.domain.User;
import com.strava.backend.enums.AuthProvider;
import com.strava.backend.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("UserRepository Integration Tests")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        user1 = User.builder()
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .password("password123")
                .authProvider(AuthProvider.LOCAL)
                .role(Role.USER)
                .active(true)
                .build();
        entityManager.persist(user1);

        user2 = User.builder()
                .email("jane@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .password("password123")
                .authProvider(AuthProvider.LOCAL)
                .role(Role.USER)
                .active(true)
                .build();
        entityManager.persist(user2);

        user3 = User.builder()
                .email("admin@example.com")
                .firstName("Admin")
                .lastName("User")
                .password("password123")
                .authProvider(AuthProvider.LOCAL)
                .role(Role.ADMIN)
                .active(false)
                .build();
        entityManager.persist(user3);

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("findByEmail Tests")
    class FindByEmailTests {

        @Test
        @DisplayName("Should find user by email")
        void shouldFindUserByEmail() {
            Optional<User> found = userRepository.findByEmail("john@example.com");

            assertThat(found).isPresent();
            assertThat(found.get().getFirstName()).isEqualTo("John");
            assertThat(found.get().getLastName()).isEqualTo("Doe");
        }

        @Test
        @DisplayName("Should return empty when email not found")
        void shouldReturnEmptyWhenEmailNotFound() {
            Optional<User> found = userRepository.findByEmail("notfound@example.com");

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should be case sensitive for email")
        void shouldBeCaseSensitiveForEmail() {
            Optional<User> found = userRepository.findByEmail("JOHN@EXAMPLE.COM");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByEmail Tests")
    class ExistsByEmailTests {

        @Test
        @DisplayName("Should return true when email exists")
        void shouldReturnTrueWhenEmailExists() {
            boolean exists = userRepository.existsByEmail("john@example.com");

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false when email does not exist")
        void shouldReturnFalseWhenEmailDoesNotExist() {
            boolean exists = userRepository.existsByEmail("notfound@example.com");

            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("countByActive Tests")
    class CountByActiveTests {

        @Test
        @DisplayName("Should count active users")
        void shouldCountActiveUsers() {
            long activeCount = userRepository.countByActive(true);

            assertThat(activeCount).isEqualTo(2);
        }

        @Test
        @DisplayName("Should count inactive users")
        void shouldCountInactiveUsers() {
            long inactiveCount = userRepository.countByActive(false);

            assertThat(inactiveCount).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Follower Relationship Tests")
    class FollowerTests {

        @Test
        @DisplayName("Should count followers")
        void shouldCountFollowers() {
            user1.getFollowers().add(user2);
            user1.getFollowers().add(user3);
            entityManager.merge(user1);
            entityManager.flush();

            int followerCount = userRepository.countFollowers(user1.getId());

            assertThat(followerCount).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return zero followers for user with no followers")
        void shouldReturnZeroFollowersForUserWithNoFollowers() {
            int followerCount = userRepository.countFollowers(user2.getId());

            assertThat(followerCount).isEqualTo(0);
        }

        @Test
        @DisplayName("Should check if user is following another")
        void shouldCheckIfUserIsFollowingAnother() {
            user1.getFollowers().add(user2);
            entityManager.merge(user1);
            entityManager.flush();

            boolean isFollowing = userRepository.isFollowing(user1.getId(), user2.getId());

            assertThat(isFollowing).isTrue();
        }

        @Test
        @DisplayName("Should return false when not following")
        void shouldReturnFalseWhenNotFollowing() {
            boolean isFollowing = userRepository.isFollowing(user1.getId(), user2.getId());

            assertThat(isFollowing).isFalse();
        }
    }

    @Nested
    @DisplayName("searchUsers Tests")
    class SearchUsersTests {

        @Test
        @DisplayName("Should find users by first name")
        void shouldFindUsersByFirstName() {
            Page<User> results = userRepository.findAll(PageRequest.of(0, 10));

            assertThat(results.getContent()).hasSize(3);
        }

        @Test
        @DisplayName("Should find all users ordered by created date")
        void shouldFindAllUsersOrderedByCreatedDate() {
            Page<User> results = userRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10));

            assertThat(results.getContent()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("findByPasswordResetCode Tests")
    class FindByPasswordResetCodeTests {

        @Test
        @DisplayName("Should find user by password reset code")
        void shouldFindUserByPasswordResetCode() {
            user1.setPasswordResetCode("RESET123");
            entityManager.merge(user1);
            entityManager.flush();

            Optional<User> found = userRepository.findByPasswordResetCode("RESET123");

            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("Should return empty for invalid reset code")
        void shouldReturnEmptyForInvalidResetCode() {
            Optional<User> found = userRepository.findByPasswordResetCode("INVALID");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("countUsersCreatedSince Tests")
    class CountUsersCreatedSinceTests {

        @Test
        @DisplayName("Should count users created since date")
        void shouldCountUsersCreatedSinceDate() {
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

            long count = userRepository.countUsersCreatedSince(yesterday);

            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("Should return zero for future date")
        void shouldReturnZeroForFutureDate() {
            LocalDateTime futureDate = LocalDateTime.now().plusDays(1);

            long count = userRepository.countUsersCreatedSince(futureDate);

            assertThat(count).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("findByIdIn Tests")
    class FindByIdInTests {

        @Test
        @DisplayName("Should find users by list of IDs")
        void shouldFindUsersByListOfIds() {
            var users = userRepository.findByIdIn(
                    java.util.List.of(user1.getId(), user2.getId()));

            assertThat(users).hasSize(2);
            assertThat(users).extracting(User::getEmail)
                    .containsExactlyInAnyOrder("john@example.com", "jane@example.com");
        }

        @Test
        @DisplayName("Should return empty list for non-existent IDs")
        void shouldReturnEmptyListForNonExistentIds() {
            var users = userRepository.findByIdIn(java.util.List.of(9999L, 9998L));

            assertThat(users).isEmpty();
        }
    }
}
