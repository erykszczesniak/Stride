package com.strava.backend.repository;

import com.strava.backend.domain.Goal;
import com.strava.backend.domain.User;
import com.strava.backend.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("GoalRepository Integration Tests")
class GoalRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private GoalRepository goalRepository;

    private User testUser;
    private Goal activeWeeklyGoal;
    private Goal activeMonthlyGoal;
    private Goal inactiveGoal;
    private Goal expiredGoal;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = User.builder()
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .password("password123")
                .authProvider(AuthProvider.LOCAL)
                .role(Role.USER)
                .active(true)
                .build();
        entityManager.persist(testUser);

        LocalDateTime now = LocalDateTime.now();

        // Active weekly goal
        activeWeeklyGoal = Goal.builder()
                .goalType(GoalType.DISTANCE)
                .period(GoalPeriod.WEEKLY)
                .activityType(ActivityType.RUN)
                .targetValue(50.0)
                .currentValue(25.0)
                .startDate(now.minusDays(3))
                .endDate(now.plusDays(4))
                .isActive(true)
                .user(testUser)
                .build();
        entityManager.persist(activeWeeklyGoal);

        // Active monthly goal
        activeMonthlyGoal = Goal.builder()
                .goalType(GoalType.TIME)
                .period(GoalPeriod.MONTHLY)
                .activityType(ActivityType.RIDE)
                .targetValue(20.0)
                .currentValue(10.0)
                .startDate(now.minusDays(10))
                .endDate(now.plusDays(20))
                .isActive(true)
                .user(testUser)
                .build();
        entityManager.persist(activeMonthlyGoal);

        // Inactive goal
        inactiveGoal = Goal.builder()
                .goalType(GoalType.ACTIVITIES)
                .period(GoalPeriod.WEEKLY)
                .activityType(ActivityType.SWIM)
                .targetValue(10.0)
                .currentValue(10.0)
                .startDate(now.minusDays(14))
                .endDate(now.minusDays(7))
                .isActive(false)
                .user(testUser)
                .build();
        entityManager.persist(inactiveGoal);

        // Expired but still marked active (for cleanup testing)
        expiredGoal = Goal.builder()
                .goalType(GoalType.ELEVATION)
                .period(GoalPeriod.WEEKLY)
                .activityType(ActivityType.HIKE)
                .targetValue(1000.0)
                .currentValue(500.0)
                .startDate(now.minusDays(14))
                .endDate(now.minusDays(1))
                .isActive(true)
                .user(testUser)
                .build();
        entityManager.persist(expiredGoal);

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("findActiveGoalsByUserId Tests")
    class FindActiveGoalsByUserIdTests {

        @Test
        @DisplayName("Should find all active goals for user ordered by end date")
        void shouldFindAllActiveGoalsForUserOrderedByEndDate() {
            List<Goal> activeGoals = goalRepository.findActiveGoalsByUserId(testUser.getId());

            assertThat(activeGoals).hasSize(3); // includes expired but still active
            assertThat(activeGoals.get(0).getPeriod()).isEqualTo(GoalPeriod.WEEKLY);
        }

        @Test
        @DisplayName("Should not include inactive goals")
        void shouldNotIncludeInactiveGoals() {
            List<Goal> activeGoals = goalRepository.findActiveGoalsByUserId(testUser.getId());

            assertThat(activeGoals).noneMatch(goal -> !goal.isActive());
        }

        @Test
        @DisplayName("Should return empty list for user with no active goals")
        void shouldReturnEmptyListForUserWithNoActiveGoals() {
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

            List<Goal> activeGoals = goalRepository.findActiveGoalsByUserId(newUser.getId());

            assertThat(activeGoals).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUserIdOrderByCreatedAtDesc Tests")
    class FindByUserIdOrderByCreatedAtDescTests {

        @Test
        @DisplayName("Should find all goals for user ordered by created date")
        void shouldFindAllGoalsForUserOrderedByCreatedDate() {
            List<Goal> goals = goalRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());

            assertThat(goals).hasSize(4);
        }

        @Test
        @DisplayName("Should include both active and inactive goals")
        void shouldIncludeBothActiveAndInactiveGoals() {
            List<Goal> goals = goalRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());

            long activeCount = goals.stream().filter(Goal::isActive).count();
            long inactiveCount = goals.stream().filter(g -> !g.isActive()).count();

            assertThat(activeCount).isEqualTo(3);
            assertThat(inactiveCount).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("findActiveGoalsByUserIdAndPeriod Tests")
    class FindActiveGoalsByUserIdAndPeriodTests {

        @Test
        @DisplayName("Should find active weekly goal")
        void shouldFindActiveWeeklyGoal() {
            LocalDateTime now = LocalDateTime.now();

            List<Goal> goals = goalRepository.findActiveGoalsByUserIdAndPeriod(
                    testUser.getId(), GoalPeriod.WEEKLY, now);

            assertThat(goals).isNotEmpty();
            assertThat(goals.getFirst().getPeriod()).isEqualTo(GoalPeriod.WEEKLY);
            assertThat(goals.getFirst().getGoalType()).isEqualTo(GoalType.DISTANCE);
        }

        @Test
        @DisplayName("Should find active monthly goal")
        void shouldFindActiveMonthlyGoal() {
            LocalDateTime now = LocalDateTime.now();

            List<Goal> goals = goalRepository.findActiveGoalsByUserIdAndPeriod(
                    testUser.getId(), GoalPeriod.MONTHLY, now);

            assertThat(goals).isNotEmpty();
            assertThat(goals.getFirst().getPeriod()).isEqualTo(GoalPeriod.MONTHLY);
        }

        @Test
        @DisplayName("Should return empty for period with no active goal")
        void shouldReturnEmptyForPeriodWithNoActiveGoal() {
            LocalDateTime now = LocalDateTime.now();

            List<Goal> goals = goalRepository.findActiveGoalsByUserIdAndPeriod(
                    testUser.getId(), GoalPeriod.YEARLY, now);

            assertThat(goals).isEmpty();
        }

        @Test
        @DisplayName("Should not find goals outside current date range")
        void shouldNotFindGoalsOutsideCurrentDateRange() {
            LocalDateTime futureDate = LocalDateTime.now().plusMonths(6);

            List<Goal> goals = goalRepository.findActiveGoalsByUserIdAndPeriod(
                    testUser.getId(), GoalPeriod.WEEKLY, futureDate);

            assertThat(goals).isEmpty();
        }

        @Test
        @DisplayName("Should return every active goal of the period, not blow up on the second")
        void shouldReturnAllActiveGoalsOfThePeriod() {
            LocalDateTime now = LocalDateTime.now();
            Goal second = Goal.builder()
                    .goalType(GoalType.DISTANCE)
                    .period(GoalPeriod.WEEKLY)
                    .activityType(ActivityType.RIDE)
                    .targetValue(100.0)
                    .currentValue(10.0)
                    .startDate(now.minusDays(1))
                    .endDate(now.plusDays(3))
                    .isActive(true)
                    .user(testUser)
                    .build();
            entityManager.persistAndFlush(second);

            List<Goal> goals = goalRepository.findActiveGoalsByUserIdAndPeriod(
                    testUser.getId(), GoalPeriod.WEEKLY, now);

            assertThat(goals).hasSizeGreaterThanOrEqualTo(2);
            assertThat(goals).allMatch(g -> g.getPeriod() == GoalPeriod.WEEKLY && g.isActive());
        }
    }

    @Nested
    @DisplayName("findExpiredGoals Tests")
    class FindExpiredGoalsTests {

        @Test
        @DisplayName("Should find expired but still active goals")
        void shouldFindExpiredButStillActiveGoals() {
            LocalDateTime now = LocalDateTime.now();

            List<Goal> expiredGoals = goalRepository.findExpiredGoals(now);

            assertThat(expiredGoals).hasSize(1);
            assertThat(expiredGoals.get(0).getGoalType()).isEqualTo(GoalType.ELEVATION);
        }

        @Test
        @DisplayName("Should not include inactive expired goals")
        void shouldNotIncludeInactiveExpiredGoals() {
            LocalDateTime now = LocalDateTime.now();

            List<Goal> expiredGoals = goalRepository.findExpiredGoals(now);

            assertThat(expiredGoals).allMatch(Goal::isActive);
        }

        @Test
        @DisplayName("Should return empty list when no expired active goals")
        void shouldReturnEmptyListWhenNoExpiredActiveGoals() {
            // Deactivate the expired goal
            expiredGoal.setActive(false);
            entityManager.merge(expiredGoal);
            entityManager.flush();

            LocalDateTime now = LocalDateTime.now();

            List<Goal> expiredGoals = goalRepository.findExpiredGoals(now);

            assertThat(expiredGoals).isEmpty();
        }
    }

    @Nested
    @DisplayName("CRUD Operations Tests")
    class CrudOperationsTests {

        @Test
        @DisplayName("Should save and retrieve goal")
        void shouldSaveAndRetrieveGoal() {
            Goal newGoal = Goal.builder()
                    .goalType(GoalType.DISTANCE)
                    .period(GoalPeriod.YEARLY)
                    .activityType(ActivityType.RUN)
                    .targetValue(1000.0)
                    .currentValue(0.0)
                    .startDate(LocalDateTime.now())
                    .endDate(LocalDateTime.now().plusYears(1))
                    .isActive(true)
                    .user(testUser)
                    .build();

            Goal saved = goalRepository.save(newGoal);
            entityManager.flush();
            entityManager.clear();

            Optional<Goal> retrieved = goalRepository.findById(saved.getId());

            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getTargetValue()).isEqualTo(1000.0);
            assertThat(retrieved.get().getPeriod()).isEqualTo(GoalPeriod.YEARLY);
        }

        @Test
        @DisplayName("Should update goal")
        void shouldUpdateGoal() {
            activeWeeklyGoal.setCurrentValue(40.0);
            goalRepository.save(activeWeeklyGoal);
            entityManager.flush();
            entityManager.clear();

            Optional<Goal> updated = goalRepository.findById(activeWeeklyGoal.getId());

            assertThat(updated).isPresent();
            assertThat(updated.get().getCurrentValue()).isEqualTo(40.0);
        }

        @Test
        @DisplayName("Should delete goal")
        void shouldDeleteGoal() {
            Long goalId = activeWeeklyGoal.getId();

            goalRepository.delete(activeWeeklyGoal);
            entityManager.flush();

            Optional<Goal> deleted = goalRepository.findById(goalId);

            assertThat(deleted).isEmpty();
        }
    }
}
