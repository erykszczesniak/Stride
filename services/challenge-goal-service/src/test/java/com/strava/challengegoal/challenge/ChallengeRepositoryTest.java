package com.strava.challengegoal.challenge;

import com.strava.challengegoal.common.ActivityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against H2 with the real Flyway chain (V1 + V2) — validates both the
 * migration scripts and the element-collection JPQL (participant joins).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("ChallengeRepository Integration Tests")
class ChallengeRepositoryTest {

    @Autowired
    private ChallengeRepository challengeRepository;

    private final LocalDateTime now = LocalDateTime.now();

    private Challenge activeChallenge;

    private Challenge challenge(String name, LocalDateTime start, LocalDateTime end,
                                boolean isPublic, Long creatorId, Set<Long> participants) {
        Challenge c = Challenge.builder()
                .name(name)
                .challengeType(ChallengeType.DISTANCE)
                .activityType(ActivityType.RUN)
                .targetValue(100.0)
                .startDate(start)
                .endDate(end)
                .isPublic(isPublic)
                .creatorId(creatorId)
                .build();
        c.getParticipantIds().addAll(participants);
        return challengeRepository.save(c);
    }

    @BeforeEach
    void setUp() {
        challengeRepository.deleteAll();
        activeChallenge = challenge("Active Running", now.minusDays(5), now.plusDays(5), true, 1L, Set.of(1L, 2L));
        challenge("Upcoming Ride", now.plusDays(10), now.plusDays(40), true, 1L, Set.of(1L));
        challenge("Private Sprint", now.minusDays(1), now.plusDays(1), false, 2L, Set.of(2L));
        challenge("Ended Walk", now.minusDays(30), now.minusDays(10), true, 1L, Set.of(1L, 3L));
    }

    @Test
    @DisplayName("Should find only active public challenges")
    void shouldFindActivePublicChallenges() {
        Page<Challenge> result = challengeRepository.findActivePublicChallenges(now, PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Challenge::getName).containsExactly("Active Running");
    }

    @Test
    @DisplayName("Should find only upcoming public challenges")
    void shouldFindUpcomingPublicChallenges() {
        Page<Challenge> result = challengeRepository.findUpcomingPublicChallenges(now, PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Challenge::getName).containsExactly("Upcoming Ride");
    }

    @Test
    @DisplayName("Should find not-yet-ended challenges the user participates in, ordered by end date")
    void shouldFindActiveChallengesByUserId() {
        List<Challenge> result = challengeRepository.findActiveChallengesByUserId(1L, now);

        assertThat(result).extracting(Challenge::getName)
                .containsExactly("Active Running", "Upcoming Ride");
    }

    @Test
    @DisplayName("Should detect participation")
    void shouldDetectParticipation() {
        assertThat(challengeRepository.isUserParticipating(activeChallenge.getId(), 2L)).isTrue();
        assertThat(challengeRepository.isUserParticipating(activeChallenge.getId(), 99L)).isFalse();
    }

    @Test
    @DisplayName("Should search challenges by name, case-insensitive")
    void shouldSearchChallengesByName() {
        Page<Challenge> result = challengeRepository.searchChallenges("running", PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Challenge::getName).containsExactly("Active Running");
    }

    @Test
    @DisplayName("Should page participant ids of a challenge")
    void shouldPageParticipantIds() {
        Page<Long> result = challengeRepository.findParticipantIdsByChallengeId(
                activeChallenge.getId(), PageRequest.of(0, 50));

        assertThat(result.getContent()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }
}
