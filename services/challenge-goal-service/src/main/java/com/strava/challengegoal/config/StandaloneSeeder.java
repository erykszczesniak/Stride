package com.strava.challengegoal.config;

import com.strava.challengegoal.challenge.Challenge;
import com.strava.challengegoal.challenge.ChallengeRepository;
import com.strava.challengegoal.challenge.ChallengeType;
import com.strava.challengegoal.common.ActivityType;
import com.strava.challengegoal.goal.Goal;
import com.strava.challengegoal.goal.GoalPeriod;
import com.strava.challengegoal.goal.GoalRepository;
import com.strava.challengegoal.goal.GoalType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@Profile("standalone")
@RequiredArgsConstructor
public class StandaloneSeeder implements CommandLineRunner {

    private final GoalRepository goals;
    private final ChallengeRepository challenges;

    @Override
    public void run(String... args) {
        log.info("[standalone] Seeding goals + challenges");
        LocalDateTime now = LocalDateTime.now();

        goals.save(Goal.builder()
                .userId(1L)
                .goalType(GoalType.DISTANCE).period(GoalPeriod.MONTHLY)
                .activityType(ActivityType.RUN).targetValue(100.0).currentValue(0.0)
                .startDate(now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0))
                .endDate(now.withDayOfMonth(1).plusMonths(1).minusNanos(1))
                .isActive(true).build());

        Challenge c = Challenge.builder()
                .name("May Distance Challenge")
                .description("Run + ride: 200km total in May")
                .challengeType(ChallengeType.DISTANCE).targetValue(200.0)
                .startDate(now.minusDays(10)).endDate(now.plusDays(20))
                .creatorId(1L).isPublic(true).build();
        c.getParticipantIds().add(1L);
        c.getParticipantIds().add(2L);
        challenges.save(c);
    }
}
