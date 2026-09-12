package com.strava.activityservice.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The repository tests run on H2 in PostgreSQL mode, which is fast but only an
 * approximation: vendor-specific migrations live in {@code db/migration/postgresql}
 * and never execute on H2. This test boots the real engine in a container, runs
 * the exact migration set production runs, and checks the seeds and the two
 * queries that depend on PostgreSQL semantics. Skipped automatically when no
 * Docker daemon is available (developer laptops without Docker); CI always has one.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Tag("integration")
@TestPropertySource(properties = {
        "spring.flyway.locations=classpath:db/migration/common,classpath:db/migration/postgresql",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
})
@DisplayName("ActivityRepository on real PostgreSQL (Testcontainers)")
class ActivityRepositoryPostgresContainerTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("production migration set applies cleanly and seeds land")
    void migrationsApplyAndSeedsLand() {
        assertThat(activityRepository.count()).isEqualTo(24);
        Integer applied = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success", Integer.class);
        assertThat(applied).isGreaterThanOrEqualTo(9);
    }

    @Test
    @DisplayName("identity sequence continues after the fixed-id sample inserts")
    void identitySequenceIsPastTheSeeds() {
        // The postgresql/ flavor restarts the identity after the seeded ids; on a
        // broken restart the first real insert would collide with activity 24.
        Long next = jdbc.queryForObject(
                "select nextval(pg_get_serial_sequence('activities', 'id'))", Long.class);
        assertThat(next).isGreaterThan(24L);
    }

    @Test
    @DisplayName("paged public listing orders newest first on the real engine")
    void publicListingOrdersNewestFirst() {
        var page = activityRepository.findAll(org.springframework.data.domain.PageRequest.of(
                0, 5, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "startTime")));
        assertThat(page.getContent()).hasSize(5);
        assertThat(page.getContent()).isSortedAccordingTo(
                (a, b) -> b.getStartTime().compareTo(a.getStartTime()));
    }
}
