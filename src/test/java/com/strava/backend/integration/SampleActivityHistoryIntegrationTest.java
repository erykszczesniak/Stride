package com.strava.backend.integration;

import com.strava.backend.domain.PersonalRecord;
import com.strava.backend.repository.PersonalRecordRepository;
import com.strava.backend.service.PersonalRecordService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the real Flyway migration set on PostgreSQL and checks the sample
 * training history seeded by V23 (the other Testcontainers tests disable
 * Flyway and let Hibernate create the schema, so nothing else exercises the
 * seeds on the production engine).
 *
 * <p>Uses its own container on purpose: {@link AbstractIntegrationTest} shares
 * a reused container whose schema is created by {@code ddl-auto=create-drop}.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("testcontainers")
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Tag("integration")
@DisplayName("Sample activity history (V23) on real PostgreSQL")
class SampleActivityHistoryIntegrationTest {

    private static final List<Long> SAMPLE_USERS = List.of(1L, 2L, 3L, 4L);
    private static final int SEEDED_ACTIVITIES = 24;
    private static final int HISTORY_ACTIVITIES = 166;
    private static final int SEEDED_PERSONAL_RECORDS = 17;

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PersonalRecordService personalRecordService;

    @Autowired
    private PersonalRecordRepository personalRecordRepository;

    @Test
    @DisplayName("every sample user has history in the previous month and in at least 10 of the last 12 weeks")
    void historyCoversTheDashboardWindows() {
        Integer total = jdbc.queryForObject("select count(*) from activities", Integer.class);
        assertThat(total).isEqualTo(SEEDED_ACTIVITIES + HISTORY_ACTIVITIES);

        Integer future = jdbc.queryForObject(
                "select count(*) from activities where start_time > now()", Integer.class);
        assertThat(future).as("no activity may be dated in the future").isZero();

        for (Long userId : SAMPLE_USERS) {
            Integer previousMonth = jdbc.queryForObject("""
                    select count(*) from activities
                    where user_id = ?
                      and start_time >= date_trunc('month', now()) - interval '1 month'
                      and start_time <  date_trunc('month', now())
                    """, Integer.class, userId);
            assertThat(previousMonth)
                    .as("user %d needs activities in the previous calendar month for the trend", userId)
                    .isGreaterThanOrEqualTo(4);

            Integer weeks = jdbc.queryForObject("""
                    select count(distinct date_trunc('week', start_time)) from activities
                    where user_id = ?
                      and start_time >= date_trunc('week', now()) - interval '11 weeks'
                    """, Integer.class, userId);
            assertThat(weeks)
                    .as("user %d should fill the 12-week volume chart", userId)
                    .isGreaterThanOrEqualTo(10);

            Integer streams = jdbc.queryForObject("""
                    select count(*) from activities
                    where user_id = ? and id > ? and route_polyline is not null
                    """, Integer.class, userId, SEEDED_ACTIVITIES);
            assertThat(streams).as("history rows are plain manual entries").isZero();
        }
    }

    @Test
    @DisplayName("recalculating personal records still yields exactly the seeded V20 rows")
    void historyDoesNotRewriteSeededRecords() {
        Map<Long, Set<String>> seeded = snapshot();
        assertThat(seeded.values().stream().mapToInt(Set::size).sum()).isEqualTo(SEEDED_PERSONAL_RECORDS);

        SAMPLE_USERS.forEach(personalRecordService::recalculateAllPRs);

        Map<Long, Set<String>> recalculated = snapshot();
        assertThat(recalculated).isEqualTo(seeded);
    }

    private Map<Long, Set<String>> snapshot() {
        return SAMPLE_USERS.stream().collect(Collectors.toMap(
                userId -> userId,
                userId -> personalRecordRepository.findByUserId(userId).stream()
                        .map(SampleActivityHistoryIntegrationTest::key)
                        .collect(Collectors.toSet())));
    }

    private static String key(PersonalRecord pr) {
        return pr.getActivityType() + "/" + pr.getDistanceType() + "/" + pr.getTimeInSeconds();
    }
}
