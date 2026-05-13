package com.strava.challengegoal.config;

import com.strava.challengegoal.domain.*;
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
    private final ChallengeParticipantRepository participants;

    @Override
    public void run(String... args) {
        log.info("[standalone] Seeding goals + challenges");
        LocalDateTime now = LocalDateTime.now();

        goals.save(Goal.builder()
                .userId(1L).title("Run 100km this month")
                .goalType(GoalType.DISTANCE).period(GoalPeriod.MONTHLY)
                .sportFilter("RUN").targetValue(100.0)
                .periodStart(now.withDayOfMonth(1)).periodEnd(now.plusMonths(1).withDayOfMonth(1))
                .active(true).build());

        Challenge c = Challenge.builder()
                .name("May Distance Challenge")
                .description("Run + ride: 200km total in May")
                .goalType(GoalType.DISTANCE).sportFilter(null).targetValue(200.0)
                .startTime(now.minusDays(10)).endTime(now.plusDays(20))
                .creatorUserId(1L).visibility(ChallengeVisibility.PUBLIC).build();
        challenges.save(c);
        participants.save(ChallengeParticipant.builder().challengeId(c.getId()).userId(1L).build());
        participants.save(ChallengeParticipant.builder().challengeId(c.getId()).userId(2L).build());
    }
}
