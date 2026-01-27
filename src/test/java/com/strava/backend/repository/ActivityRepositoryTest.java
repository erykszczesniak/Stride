package com.strava.backend.repository;

import com.strava.backend.domain.Activity;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ActivityRepository Integration Tests")
class ActivityRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ActivityRepository activityRepository;

    private User testUser;
    private User anotherUser;
    private Activity activity1;
    private Activity activity2;
    private Activity activity3;

    @BeforeEach
    void setUp() {
        // Create test users
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
                .password("password123")
                .authProvider(AuthProvider.LOCAL)
                .role(Role.USER)
                .active(true)
                .build();
        entityManager.persist(anotherUser);

        // Create test activities
        LocalDateTime now = LocalDateTime.now();

        activity1 = Activity.builder()
                .title("Morning Run")
                .description("A nice morning run")
                .activityType(ActivityType.RUN)
                .distance(10.0)
                .duration(3600)
                .elevationGain(100)
                .calories(500)
                .startTime(now.minusDays(1))
                .isPrivate(false)
                .user(testUser)
                .build();
        entityManager.persist(activity1);

        activity2 = Activity.builder()
                .title("Evening Ride")
                .description("Cycling session")
                .activityType(ActivityType.RIDE)
                .distance(25.0)
                .duration(5400)
                .elevationGain(200)
                .calories(800)
                .startTime(now.minusDays(2))
                .isPrivate(false)
                .user(testUser)
                .build();
        entityManager.persist(activity2);

        activity3 = Activity.builder()
                .title("Private Swim")
                .description("Pool session")
                .activityType(ActivityType.SWIM)
                .distance(2.0)
                .duration(1800)
                .elevationGain(0)
                .calories(300)
                .startTime(now.minusDays(3))
                .isPrivate(true)
                .user(anotherUser)
                .build();
        entityManager.persist(activity3);

        entityManager.flush();
    }

    @Nested
    @DisplayName("findByUserIdOrderByCreatedAtDesc Tests")
    class FindByUserIdTests {

        @Test
        @DisplayName("Should find activities by user id ordered by created date")
        void shouldFindActivitiesByUserIdOrdered() {
            List<Activity> activities = activityRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());

            assertThat(activities).hasSize(2);
            assertThat(activities.get(0).getTitle()).isEqualTo("Evening Ride");
            assertThat(activities.get(1).getTitle()).isEqualTo("Morning Run");
        }

        @Test
        @DisplayName("Should return empty list for user with no activities")
        void shouldReturnEmptyListForUserWithNoActivities() {
            User newUser = User.builder()
                    .email("new@example.com")
                    .firstName("New")
                    .lastName("User")
                    .password("password123")
                    .authProvider(AuthProvider.LOCAL)
                    .role(Role.USER)
                    .active(true)
                    .build();
            entityManager.persist(newUser);
            entityManager.flush();

            List<Activity> activities = activityRepository.findByUserIdOrderByCreatedAtDesc(newUser.getId());

            assertThat(activities).isEmpty();
        }
    }

    @Nested
    @DisplayName("Sum and Count Queries Tests")
    class SumAndCountTests {

        @Test
        @DisplayName("Should sum distance by user and date range")
        void shouldSumDistanceByUserAndDateRange() {
            LocalDateTime startDate = LocalDateTime.now().minusDays(10);
            LocalDateTime endDate = LocalDateTime.now().plusDays(1);

            Optional<Double> totalDistance = activityRepository.sumDistanceByUserAndDateRange(
                    testUser.getId(), startDate, endDate);

            assertThat(totalDistance).isPresent();
            assertThat(totalDistance.get()).isEqualTo(35.0); // 10 + 25
        }

        @Test
        @DisplayName("Should sum duration by user and date range")
        void shouldSumDurationByUserAndDateRange() {
            LocalDateTime startDate = LocalDateTime.now().minusDays(10);
            LocalDateTime endDate = LocalDateTime.now().plusDays(1);

            Optional<Long> totalDuration = activityRepository.sumDurationByUserAndDateRange(
                    testUser.getId(), startDate, endDate);

            assertThat(totalDuration).isPresent();
            assertThat(totalDuration.get()).isEqualTo(9000L); // 3600 + 5400
        }

        @Test
        @DisplayName("Should sum calories by user and date range")
        void shouldSumCaloriesByUserAndDateRange() {
            LocalDateTime startDate = LocalDateTime.now().minusDays(10);
            LocalDateTime endDate = LocalDateTime.now().plusDays(1);

            Optional<Integer> totalCalories = activityRepository.sumCaloriesByUserAndDateRange(
                    testUser.getId(), startDate, endDate);

            assertThat(totalCalories).isPresent();
            assertThat(totalCalories.get()).isEqualTo(1300); // 500 + 800
        }

        @Test
        @DisplayName("Should sum elevation by user and date range")
        void shouldSumElevationByUserAndDateRange() {
            LocalDateTime startDate = LocalDateTime.now().minusDays(10);
            LocalDateTime endDate = LocalDateTime.now().plusDays(1);

            Optional<Integer> totalElevation = activityRepository.sumElevationByUserAndDateRange(
                    testUser.getId(), startDate, endDate);

            assertThat(totalElevation).isPresent();
            assertThat(totalElevation.get()).isEqualTo(300); // 100 + 200
        }

        @Test
        @DisplayName("Should count activities by user and date range")
        void shouldCountActivitiesByUserAndDateRange() {
            LocalDateTime startDate = LocalDateTime.now().minusDays(10);
            LocalDateTime endDate = LocalDateTime.now().plusDays(1);

            int count = activityRepository.countByUserAndDateRange(
                    testUser.getId(), startDate, endDate);

            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return empty optional when no activities in date range")
        void shouldReturnEmptyOptionalWhenNoActivitiesInDateRange() {
            LocalDateTime startDate = LocalDateTime.now().minusYears(2);
            LocalDateTime endDate = LocalDateTime.now().minusYears(1);

            Optional<Double> totalDistance = activityRepository.sumDistanceByUserAndDateRange(
                    testUser.getId(), startDate, endDate);

            assertThat(totalDistance).isEmpty();
        }
    }

    @Nested
    @DisplayName("countByUserId Tests")
    class CountByUserIdTests {

        @Test
        @DisplayName("Should count all activities for user")
        void shouldCountAllActivitiesForUser() {
            int count = activityRepository.countByUserId(testUser.getId());

            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return zero for user with no activities")
        void shouldReturnZeroForUserWithNoActivities() {
            User newUser = User.builder()
                    .email("empty@example.com")
                    .firstName("Empty")
                    .lastName("User")
                    .password("password123")
                    .authProvider(AuthProvider.LOCAL)
                    .role(Role.USER)
                    .active(true)
                    .build();
            entityManager.persist(newUser);
            entityManager.flush();

            int count = activityRepository.countByUserId(newUser.getId());

            assertThat(count).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("isLikedByUser Tests")
    class IsLikedByUserTests {

        @Test
        @DisplayName("Should return true when activity is liked by user")
        void shouldReturnTrueWhenActivityIsLikedByUser() {
            activity1.getLikedBy().add(anotherUser);
            entityManager.persist(activity1);
            entityManager.flush();

            boolean isLiked = activityRepository.isLikedByUser(activity1.getId(), anotherUser.getId());

            assertThat(isLiked).isTrue();
        }

        @Test
        @DisplayName("Should return false when activity is not liked by user")
        void shouldReturnFalseWhenActivityIsNotLikedByUser() {
            boolean isLiked = activityRepository.isLikedByUser(activity1.getId(), anotherUser.getId());

            assertThat(isLiked).isFalse();
        }
    }

    @Nested
    @DisplayName("searchActivities Tests")
    class SearchActivitiesTests {

        @Test
        @DisplayName("Should find activities by title")
        void shouldFindActivitiesByTitle() {
            Page<Activity> results = activityRepository.searchActivities("Morning", PageRequest.of(0, 10));

            assertThat(results.getContent()).hasSize(1);
            assertThat(results.getContent().get(0).getTitle()).isEqualTo("Morning Run");
        }

        @Test
        @DisplayName("Should find activities by description")
        void shouldFindActivitiesByDescription() {
            Page<Activity> results = activityRepository.searchActivities("cycling", PageRequest.of(0, 10));

            assertThat(results.getContent()).hasSize(1);
            assertThat(results.getContent().get(0).getTitle()).isEqualTo("Evening Ride");
        }

        @Test
        @DisplayName("Should not find private activities")
        void shouldNotFindPrivateActivities() {
            Page<Activity> results = activityRepository.searchActivities("Pool", PageRequest.of(0, 10));

            assertThat(results.getContent()).isEmpty();
        }

        @Test
        @DisplayName("Should be case insensitive")
        void shouldBeCaseInsensitive() {
            Page<Activity> results = activityRepository.searchActivities("MORNING", PageRequest.of(0, 10));

            assertThat(results.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findByUserIdAndActivityType Tests")
    class FindByUserIdAndActivityTypeTests {

        @Test
        @DisplayName("Should find activities by user and activity type")
        void shouldFindActivitiesByUserAndActivityType() {
            Page<Activity> results = activityRepository.findByUserIdAndActivityType(
                    testUser.getId(), ActivityType.RUN, PageRequest.of(0, 10));

            assertThat(results.getContent()).hasSize(1);
            assertThat(results.getContent().get(0).getActivityType()).isEqualTo(ActivityType.RUN);
        }

        @Test
        @DisplayName("Should return empty when no activities of type")
        void shouldReturnEmptyWhenNoActivitiesOfType() {
            Page<Activity> results = activityRepository.findByUserIdAndActivityType(
                    testUser.getId(), ActivityType.SWIM, PageRequest.of(0, 10));

            assertThat(results.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("searchUserActivities Tests")
    class SearchUserActivitiesTests {

        @Test
        @DisplayName("Should find user activities by title")
        void shouldFindUserActivitiesByTitle() {
            Page<Activity> results = activityRepository.searchUserActivities(
                    testUser.getId(), "Morning", PageRequest.of(0, 10));

            assertThat(results.getContent()).hasSize(1);
            assertThat(results.getContent().get(0).getTitle()).isEqualTo("Morning Run");
        }

        @Test
        @DisplayName("Should find user activities by description")
        void shouldFindUserActivitiesByDescription() {
            Page<Activity> results = activityRepository.searchUserActivities(
                    testUser.getId(), "cycling", PageRequest.of(0, 10));

            assertThat(results.getContent()).hasSize(1);
            assertThat(results.getContent().get(0).getTitle()).isEqualTo("Evening Ride");
        }

        @Test
        @DisplayName("Should only find activities belonging to user")
        void shouldOnlyFindActivitiesBelongingToUser() {
            Page<Activity> results = activityRepository.searchUserActivities(
                    testUser.getId(), "Pool", PageRequest.of(0, 10));

            assertThat(results.getContent()).isEmpty();
        }

        @Test
        @DisplayName("Should be case insensitive")
        void shouldBeCaseInsensitive() {
            Page<Activity> results = activityRepository.searchUserActivities(
                    testUser.getId(), "MORNING", PageRequest.of(0, 10));

            assertThat(results.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should return empty when no matches")
        void shouldReturnEmptyWhenNoMatches() {
            Page<Activity> results = activityRepository.searchUserActivities(
                    testUser.getId(), "nonexistent", PageRequest.of(0, 10));

            assertThat(results.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("sumDistanceByUserId Tests")
    class SumDistanceByUserIdTests {

        @Test
        @DisplayName("Should sum all distances for user")
        void shouldSumAllDistancesForUser() {
            Double totalDistance = activityRepository.sumDistanceByUserId(testUser.getId());

            assertThat(totalDistance).isEqualTo(35.0); // 10.0 + 25.0
        }

        @Test
        @DisplayName("Should return zero for user with no activities")
        void shouldReturnZeroForUserWithNoActivities() {
            User newUser = User.builder()
                    .email("empty@example.com")
                    .firstName("Empty")
                    .lastName("User")
                    .password("password123")
                    .authProvider(AuthProvider.LOCAL)
                    .role(Role.USER)
                    .active(true)
                    .build();
            entityManager.persist(newUser);
            entityManager.flush();

            Double totalDistance = activityRepository.sumDistanceByUserId(newUser.getId());

            assertThat(totalDistance).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("sumDurationByUserId Tests")
    class SumDurationByUserIdTests {

        @Test
        @DisplayName("Should sum all durations for user")
        void shouldSumAllDurationsForUser() {
            Long totalDuration = activityRepository.sumDurationByUserId(testUser.getId());

            assertThat(totalDuration).isEqualTo(9000L); // 3600 + 5400
        }

        @Test
        @DisplayName("Should return zero for user with no activities")
        void shouldReturnZeroForUserWithNoActivities() {
            User newUser = User.builder()
                    .email("noduration@example.com")
                    .firstName("No")
                    .lastName("Duration")
                    .password("password123")
                    .authProvider(AuthProvider.LOCAL)
                    .role(Role.USER)
                    .active(true)
                    .build();
            entityManager.persist(newUser);
            entityManager.flush();

            Long totalDuration = activityRepository.sumDurationByUserId(newUser.getId());

            assertThat(totalDuration).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("Challenge Progress Queries Tests")
    class ChallengeProgressQueriesTests {

        @Test
        @DisplayName("Should sum distance for challenge with activity type filter")
        void shouldSumDistanceForChallengeWithActivityTypeFilter() {
            LocalDateTime startDate = LocalDateTime.now().minusDays(10);
            LocalDateTime endDate = LocalDateTime.now().plusDays(1);

            Double distance = activityRepository.sumDistanceForChallenge(
                    testUser.getId(), ActivityType.RUN, startDate, endDate);

            assertThat(distance).isEqualTo(10.0);
        }

        @Test
        @DisplayName("Should sum distance for challenge without activity type filter")
        void shouldSumDistanceForChallengeWithoutActivityTypeFilter() {
            LocalDateTime startDate = LocalDateTime.now().minusDays(10);
            LocalDateTime endDate = LocalDateTime.now().plusDays(1);

            Double distance = activityRepository.sumDistanceForChallenge(
                    testUser.getId(), null, startDate, endDate);

            assertThat(distance).isEqualTo(35.0);
        }

        @Test
        @DisplayName("Should count activities for challenge")
        void shouldCountActivitiesForChallenge() {
            LocalDateTime startDate = LocalDateTime.now().minusDays(10);
            LocalDateTime endDate = LocalDateTime.now().plusDays(1);

            Double count = activityRepository.countActivitiesForChallenge(
                    testUser.getId(), null, startDate, endDate);

            assertThat(count).isEqualTo(2.0);
        }
    }
}
