package com.strava.challengegoal.goal;

import com.strava.challengegoal.common.ActivityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against H2 with the real Flyway chain (V1 + V2) — validates the
 * reshaped goals table against the monolith-parity entity mapping.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("GoalRepository Integration Tests")
class GoalRepositoryTest {

    @Autowired
    private GoalRepository goalRepository;

    private final LocalDateTime now = LocalDateTime.now();

    private Goal goal(Long userId, boolean active, LocalDateTime endDate) {
        return goalRepository.save(Goal.builder()
                .goalType(GoalType.DISTANCE)
                .period(GoalPeriod.WEEKLY)
                .activityType(ActivityType.RUN)
                .targetValue(50.0)
                .currentValue(10.0)
                .startDate(now.minusDays(3))
                .endDate(endDate)
                .isActive(active)
                .userId(userId)
                .build());
    }

    @BeforeEach
    void setUp() {
        goalRepository.deleteAll();
    }

    @Test
    @DisplayName("Should find only active goals for a user, ordered by end date")
    void shouldFindActiveGoalsByUserId() {
        goal(1L, true, now.plusDays(10));
        Goal endsSooner = goal(1L, true, now.plusDays(2));
        goal(1L, false, now.plusDays(5));
        goal(2L, true, now.plusDays(1));

        List<Goal> result = goalRepository.findActiveGoalsByUserId(1L);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().getId()).isEqualTo(endsSooner.getId());
        assertThat(result).allMatch(Goal::isActive);
    }

    @Test
    @DisplayName("Should return all goals for a user including inactive")
    void shouldReturnAllGoalsForUser() {
        goal(1L, true, now.plusDays(10));
        goal(1L, false, now.plusDays(5));
        goal(2L, true, now.plusDays(1));

        List<Goal> result = goalRepository.findByUserIdOrderByCreatedAtDesc(1L);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("Should persist computed-progress source fields")
    void shouldPersistProgressFields() {
        Goal saved = goal(1L, true, now.plusDays(10));

        Goal reloaded = goalRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getCurrentValue()).isEqualTo(10.0);
        assertThat(reloaded.getProgressPercentage()).isEqualTo(20.0);
        assertThat(reloaded.getRemainingValue()).isEqualTo(40.0);
        assertThat(reloaded.isCompleted()).isFalse();
        assertThat(reloaded.getCreatedAt()).isNotNull();
    }
}
