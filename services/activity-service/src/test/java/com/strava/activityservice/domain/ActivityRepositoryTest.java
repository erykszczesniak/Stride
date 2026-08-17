package com.strava.activityservice.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against H2 (MODE=PostgreSQL) with the real Flyway migrations —
 * including the sample seeds (activities 1..24, streams, PRs) — so this both
 * exercises the non-trivial queries and proves the migrations are
 * H2-compatible end to end.
 */
@DataJpaTest
@DisplayName("ActivityRepository (H2 + Flyway migrations)")
class ActivityRepositoryTest {

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private PersonalRecordRepository personalRecordRepository;

    @Autowired
    private UserCacheRepository userCacheRepository;

    @Test
    @DisplayName("sample seeds land: 24 activities with fixed ids, PRs, user cache")
    void sampleSeedsLand() {
        assertThat(activityRepository.count()).isEqualTo(24);
        assertThat(activityRepository.findById(1L)).hasValueSatisfying(a -> {
            assertThat(a.getTitle()).isEqualTo("Morning Trail Run");
            assertThat(a.getUserId()).isEqualTo(1L);
            // V4 stream enrichment applied
            assertThat(a.getRoutePolyline()).isNotBlank();
            assertThat(a.getElevationProfile()).startsWith("[");
            assertThat(a.getAverageHeartRate()).isEqualTo(152);
            // V5 Java migration anchored the timestamps to start_time
            assertThat(a.getPointTimestamps()).startsWith("[");
        });
        // ride 2 got power, indoor swim 3 got HR + normalized encoded polyline
        assertThat(activityRepository.findById(2L)).hasValueSatisfying(a ->
                assertThat(a.getAveragePower()).isEqualTo(178));
        assertThat(activityRepository.findById(3L)).hasValueSatisfying(a -> {
            assertThat(a.getAverageHeartRate()).isEqualTo(128);
            assertThat(a.getRoutePolyline()).isEqualTo("sbx}Hs{d_C");
        });

        assertThat(personalRecordRepository.count()).isEqualTo(17);
        assertThat(userCacheRepository.findById(1L)).hasValueSatisfying(u ->
                assertThat(u.getFirstName()).isEqualTo("Pan"));
    }

    @Test
    @DisplayName("point timestamps align 1:1 with the seeded polylines")
    void seededStreamsAligned() {
        Activity a = activityRepository.findById(1L).orElseThrow();
        int points = com.strava.activityservice.util.GeoUtils.decodePolyline(a.getRoutePolyline()).size();
        long[] timestamps = com.strava.activityservice.util.StreamJson.parsePointTimestamps(a.getPointTimestamps());
        double[] elevation = com.strava.activityservice.util.StreamJson.parseElevationProfile(a.getElevationProfile());
        assertThat(timestamps).hasSize(points);
        assertThat(elevation).hasSize(points);
    }

    @Test
    @DisplayName("the H2 identity restart lets new inserts continue after the seeded ids")
    void identityContinuesAfterSeeds() {
        Activity fresh = Activity.builder()
                .userId(99L)
                .title("Fresh")
                .activityType(ActivityType.RUN)
                .distance(3.0)
                .duration(1000)
                .startTime(LocalDateTime.now())
                .build();
        Activity saved = activityRepository.saveAndFlush(fresh);
        assertThat(saved.getId()).isGreaterThanOrEqualTo(25L);
    }

    @Test
    @DisplayName("findByUserId orders by createdAt desc and pages")
    void findByUserIdOrdersByCreatedAtDesc() {
        Page<Activity> page = activityRepository.findByUserId(1L, PageRequest.of(0, 3));
        assertThat(page.getTotalElements()).isEqualTo(6);
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getContent().get(0).getCreatedAt())
                .isAfterOrEqualTo(page.getContent().get(1).getCreatedAt());
    }

    @Test
    @DisplayName("search matches title and description case-insensitively")
    void searchUserActivities() {
        Page<Activity> byTitle = activityRepository.searchUserActivities(1L, "trail", PageRequest.of(0, 10));
        assertThat(byTitle.getContent()).extracting(Activity::getId).containsExactly(1L);

        Page<Activity> byDescription = activityRepository.searchUserActivities(1L, "SUNRISE", PageRequest.of(0, 10));
        assertThat(byDescription.getContent()).extracting(Activity::getId).containsExactly(1L);

        Page<Activity> typed = activityRepository.searchUserActivitiesByType(
                1L, "e", ActivityType.RIDE, PageRequest.of(0, 10));
        assertThat(typed.getContent()).allSatisfy(a -> assertThat(a.getActivityType()).isEqualTo(ActivityType.RIDE));
    }

    @Test
    @DisplayName("lifetime sums COALESCE to zero for unknown users")
    void lifetimeSums() {
        assertThat(activityRepository.sumDistanceByUserId(12345L)).isEqualTo(0.0);
        assertThat(activityRepository.sumDurationByUserId(12345L)).isEqualTo(0L);
        assertThat(activityRepository.countByUserId(1L)).isEqualTo(6);
        // user 1: 8.5 + 45.2 + 1.5 + 6.0 + 22.0 + 5.0
        assertThat(activityRepository.sumDistanceByUserId(1L)).isCloseTo(88.2, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    @DisplayName("the native DOW aggregation runs on H2 in PostgreSQL mode")
    void dailyStatsNativeQueryRuns() {
        LocalDateTime from = LocalDateTime.now().minusDays(30);
        LocalDateTime to = LocalDateTime.now().plusDays(1);
        List<Object[]> rows = activityRepository.getDailyStatsByUserAndDateRange(1L, from, to);

        assertThat(rows).isNotEmpty();
        double total = 0;
        for (Object[] row : rows) {
            int dow = ((Number) row[0]).intValue();
            assertThat(dow).isBetween(0, 6);
            total += ((Number) row[1]).doubleValue();
        }
        assertThat(total).isCloseTo(88.2, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    @DisplayName("the native monthly aggregation returns the five expected columns")
    void aggregatedStatsNativeQueryRuns() {
        LocalDateTime from = LocalDateTime.now().minusDays(30);
        LocalDateTime to = LocalDateTime.now().plusDays(1);
        Object[] raw = activityRepository.getAggregatedStatsByUserAndDateRange(1L, from, to);
        Object[] stats = (raw != null && raw.length > 0 && raw[0] instanceof Object[])
                ? (Object[]) raw[0] : raw;

        assertThat(stats).hasSize(5);
        assertThat(((Number) stats[0]).doubleValue()).isCloseTo(88.2, org.assertj.core.data.Offset.offset(0.001));
        assertThat(((Number) stats[4]).intValue()).isEqualTo(6);
    }

    @Test
    @DisplayName("PR recalculation source query orders by startTime ascending")
    void recalculationSourceOrdered() {
        List<Activity> activities = activityRepository.findAllByUserIdOrderByStartTimeAsc(1L);
        assertThat(activities).hasSize(6);
        for (int i = 1; i < activities.size(); i++) {
            assertThat(activities.get(i).getStartTime())
                    .isAfterOrEqualTo(activities.get(i - 1).getStartTime());
        }
    }

    @Test
    @DisplayName("findByUserIdAndStartTimeBetween filters on startTime, inclusive on both ends")
    void startTimeWindowIsInclusive() {
        Activity seeded = activityRepository.findById(1L).orElseThrow();

        // A degenerate [startTime, startTime] window still matches — BETWEEN
        // is inclusive on both bounds, the rule the internal user-activity
        // summary aggregation relies on.
        List<Activity> window = activityRepository.findByUserIdAndStartTimeBetween(
                seeded.getUserId(), seeded.getStartTime(), seeded.getStartTime());
        assertThat(window).extracting(Activity::getId).contains(1L);

        // Just outside either bound -> excluded.
        assertThat(activityRepository.findByUserIdAndStartTimeBetween(
                seeded.getUserId(),
                seeded.getStartTime().plusSeconds(1),
                seeded.getStartTime().plusHours(1)))
                .extracting(Activity::getId).doesNotContain(1L);
        assertThat(activityRepository.findByUserIdAndStartTimeBetween(
                seeded.getUserId(),
                seeded.getStartTime().minusHours(1),
                seeded.getStartTime().minusSeconds(1)))
                .extracting(Activity::getId).doesNotContain(1L);
    }

    @Test
    @DisplayName("seeded PRs mirror what recalculateAllPRs derives (user 1 spot checks)")
    void seededPRs() {
        List<PersonalRecord> user1 = personalRecordRepository.findByUserId(1L);
        assertThat(user1).hasSize(3);
        assertThat(user1)
                .anySatisfy(pr -> {
                    assertThat(pr.getActivityType()).isEqualTo(ActivityType.RUN);
                    assertThat(pr.getDistanceType()).isEqualTo(DistanceType.FIVE_KM);
                    assertThat(pr.getTimeInSeconds()).isEqualTo(1500);
                    assertThat(pr.getPreviousTimeInSeconds()).isEqualTo(1694);
                });
    }
}
