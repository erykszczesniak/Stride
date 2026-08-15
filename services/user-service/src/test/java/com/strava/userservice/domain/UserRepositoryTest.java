package com.strava.userservice.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository queries against the real Flyway schema on H2 (MODE=PostgreSQL), including
 * the V3 seed parity data (users 1..4 + follow edges from the monolith's sample set)
 * and the V4 identity restart.
 */
@DataJpaTest
@DisplayName("UserRepository")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserFollowRepository followRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User newUser(String firstName, String lastName, String email, boolean hidden) {
        return User.builder()
                .firstName(firstName).lastName(lastName).email(email)
                .authProvider(AuthProvider.LOCAL).role(Role.USER).active(true)
                .hideFromSearch(hidden)
                .build();
    }

    @Nested
    @DisplayName("seed parity (V3/V4 migrations)")
    class SeedParity {

        @Test
        void seedsTheFourMonolithSampleUsersWithFixedIds() {
            assertThat(userRepository.findById(1L)).hasValueSatisfying(u -> {
                assertThat(u.getEmail()).isEqualTo("pan@example.com");
                assertThat(u.getFirstName()).isEqualTo("Pan");
                assertThat(u.getRole()).isEqualTo(Role.USER);
            });
            assertThat(userRepository.findById(4L)).hasValueSatisfying(u -> {
                assertThat(u.getEmail()).isEqualTo("admin@example.com");
                assertThat(u.getRole()).isEqualTo(Role.ADMIN);
            });
        }

        @Test
        void seedsTheMonolithFollowEdges() {
            // user 1 is followed by 2 and 3; user 4 is followed by 1, 2, 3.
            assertThat(followRepository.countByUserId(1L)).isEqualTo(2);
            assertThat(followRepository.countByUserId(4L)).isEqualTo(3);
            // user 4 follows nobody.
            assertThat(followRepository.countByFollowerId(4L)).isZero();
            assertThat(followRepository.existsByUserIdAndFollowerId(2L, 1L)).isTrue();
        }

        @Test
        void identityContinuesAfterSeedIds() {
            User saved = userRepository.saveAndFlush(
                    newUser("New", "Person", "new@example.com", false));

            assertThat(saved.getId()).isGreaterThanOrEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("searchUsersByText")
    class SearchUsersByText {

        @Test
        void matchesCaseInsensitivelyOnNamesAndEmail() {
            Page<User> byName = userRepository.searchUsersByText("jAnE", PageRequest.of(0, 10));
            assertThat(byName.getContent()).extracting(User::getEmail)
                    .contains("jane@example.com");

            Page<User> byEmail = userRepository.searchUsersByText("admin@", PageRequest.of(0, 10));
            assertThat(byEmail.getContent()).extracting(User::getId).containsExactly(4L);
        }

        @Test
        void excludesUsersHiddenFromSearch() {
            userRepository.saveAndFlush(newUser("Hidden", "Person", "hidden@example.com", true));
            userRepository.saveAndFlush(newUser("Visible", "Person", "visible@example.com", false));

            Page<User> result = userRepository.searchUsersByText("Person", PageRequest.of(0, 10));

            assertThat(result.getContent()).extracting(User::getEmail)
                    .containsExactly("visible@example.com");
        }
    }

    @Nested
    @DisplayName("findSuggestedUsers")
    class FindSuggestedUsers {

        @Test
        void excludesSelfAndAlreadyFollowedOrderedByFollowerCount() {
            // Admin (4) follows nobody -> everyone else is suggestible; users 1..3 all
            // have 2 followers, so the id tie-break yields 1, 2, 3.
            List<User> suggested = userRepository.findSuggestedUsers(4L, PageRequest.of(0, 10));

            assertThat(suggested).extracting(User::getId).startsWith(1L, 2L, 3L)
                    .doesNotContain(4L);
        }

        @Test
        void mostFollowedComesFirst() {
            // A fresh user with zero followers must rank below the seeded ones.
            User fresh = userRepository.saveAndFlush(
                    newUser("Fresh", "User", "fresh@example.com", false));
            entityManager.clear();

            List<User> suggested = userRepository.findSuggestedUsers(4L, PageRequest.of(0, 10));

            assertThat(suggested).extracting(User::getId)
                    .containsExactly(1L, 2L, 3L, fresh.getId());
        }

        @Test
        void userFollowingEveryoneGetsOnlyUnfollowedNewcomers() {
            // User 1 follows 2, 3, 4 (seed), so only newcomers remain.
            User fresh = userRepository.saveAndFlush(
                    newUser("Fresh", "User", "fresh2@example.com", false));

            List<User> suggested = userRepository.findSuggestedUsers(1L, PageRequest.of(0, 10));

            assertThat(suggested).extracting(User::getId).containsExactly(fresh.getId());
        }
    }

    @Nested
    @DisplayName("weekly summary recipients")
    class WeeklySummaryRecipients {

        @Test
        void onlyActiveOptedInUsersAreReturned() {
            User optedOut = userRepository.findById(1L).orElseThrow();
            optedOut.setWeeklySummary(false);
            User inactive = userRepository.findById(2L).orElseThrow();
            inactive.setActive(false);
            userRepository.saveAndFlush(optedOut);
            userRepository.saveAndFlush(inactive);

            Page<User> recipients =
                    userRepository.findByWeeklySummaryTrueAndActiveTrue(PageRequest.of(0, 100));

            assertThat(recipients.getContent()).extracting(User::getId)
                    .containsExactlyInAnyOrder(3L, 4L);
        }
    }

    @Nested
    @DisplayName("auth lookups")
    class AuthLookups {

        @Test
        void findsUserByProviderIdentity() {
            User stravaUser = newUser("Athlete", "One", "strava_12345@stride.local", false);
            stravaUser.setAuthProvider(AuthProvider.STRAVA);
            stravaUser.setProviderId("12345");
            userRepository.saveAndFlush(stravaUser);

            assertThat(userRepository.findByProviderIdAndAuthProvider("12345", AuthProvider.STRAVA))
                    .isPresent();
            assertThat(userRepository.findByProviderIdAndAuthProvider("12345", AuthProvider.GOOGLE))
                    .isEmpty();
        }

        @Test
        void findsUserByPasswordResetCode() {
            User u = userRepository.findById(1L).orElseThrow();
            u.setPasswordResetCode("code-123");
            userRepository.saveAndFlush(u);

            assertThat(userRepository.findByPasswordResetCode("code-123"))
                    .hasValueSatisfying(found -> assertThat(found.getId()).isEqualTo(1L));
            assertThat(userRepository.findByPasswordResetCode("nope")).isEmpty();
        }
    }
}
