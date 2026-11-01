package com.strava.activityservice.domain;

import com.strava.activityservice.client.UserVisibilityClient;
import com.strava.activityservice.event.ActivityEventPublisher;
import com.strava.activityservice.pr.PersonalRecordService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against H2 (MODE=PostgreSQL) with the real Flyway migrations —
 * including the sample seeds (activities 1..24 with streams and PRs, plus the
 * 166 history rows of V10) — so this both exercises the non-trivial queries
 * and proves the migrations are H2-compatible end to end.
 */
@DataJpaTest
@Import(PersonalRecordService.class)
@DisplayName("ActivityRepository (H2 + Flyway migrations)")
class ActivityRepositoryTest {

    private static final int SEEDED_ACTIVITIES = 24;
    private static final int HISTORY_ACTIVITIES = 166;
    private static final int USER_1_ACTIVITIES = 6 + 39;
    private static final List<Long> SAMPLE_USERS = List.of(1L, 2L, 3L, 4L);

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private PersonalRecordRepository personalRecordRepository;

    @Autowired
    private UserCacheRepository userCacheRepository;

    @Autowired
    private PersonalRecordService personalRecordService;

    @MockitoBean
    private UserVisibilityClient userVisibilityClient;

    @MockitoBean
    private ActivityEventPublisher activityEventPublisher;

    @Test
    @DisplayName("sample seeds land: 24 fixed-id activities + 166 history rows, PRs, user cache")
    void sampleSeedsLand() {
        assertThat(activityRepository.count()).isEqualTo(SEEDED_ACTIVITIES + HISTORY_ACTIVITIES);
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
        assertThat(page.getTotalElements()).isEqualTo(USER_1_ACTIVITIES);
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
        assertThat(activityRepository.countByUserId(1L)).isEqualTo(USER_1_ACTIVITIES);
        double expected = user1Distance(LocalDateTime.MIN, LocalDateTime.MAX);
        assertThat(expected).isGreaterThan(88.2); // 88.2 km from V3 plus the V10 history
        assertThat(activityRepository.sumDistanceByUserId(1L)).isCloseTo(expected, org.assertj.core.data.Offset.offset(0.001));
    }

    /** Sum of user 1's distances with start_time in [from, to], computed entity-side as the oracle. */
    private double user1Distance(LocalDateTime from, LocalDateTime to) {
        return activityRepository.findAllByUserIdOrderByStartTimeAsc(1L).stream()
                .filter(a -> !a.getStartTime().isBefore(from) && !a.getStartTime().isAfter(to))
                .mapToDouble(Activity::getDistance)
                .sum();
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
        assertThat(total).isCloseTo(user1Distance(from, to), org.assertj.core.data.Offset.offset(0.001));
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
        assertThat(((Number) stats[0]).doubleValue()).isCloseTo(user1Distance(from, to), org.assertj.core.data.Offset.offset(0.001));
        long expectedCount = activityRepository.findAllByUserIdOrderByStartTimeAsc(1L).stream()
                .filter(a -> !a.getStartTime().isBefore(from) && !a.getStartTime().isAfter(to))
                .count();
        assertThat(((Number) stats[4]).intValue()).isEqualTo((int) expectedCount);
    }

    @Test
    @DisplayName("PR recalculation source query orders by startTime ascending")
    void recalculationSourceOrdered() {
        List<Activity> activities = activityRepository.findAllByUserIdOrderByStartTimeAsc(1L);
        assertThat(activities).hasSize(USER_1_ACTIVITIES);
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

    @Test
    @DisplayName("V10 history covers the previous month and at least 10 of the last 12 weeks for every sample user")
    void historyCoversTheDashboardWindows() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate thisMonday = now.toLocalDate().with(DayOfWeek.MONDAY);
        LocalDateTime chartStart = thisMonday.minusWeeks(11).atStartOfDay();
        LocalDateTime monthStart = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime previousMonthStart = monthStart.minusMonths(1);

        for (Long userId : SAMPLE_USERS) {
            List<Activity> all = activityRepository.findAllByUserIdOrderByStartTimeAsc(userId);
            assertThat(all).as("user %d", userId).allSatisfy(a -> assertThat(a.getStartTime()).isBeforeOrEqualTo(now));

            long previousMonth = all.stream()
                    .filter(a -> !a.getStartTime().isBefore(previousMonthStart) && a.getStartTime().isBefore(monthStart))
                    .count();
            assertThat(previousMonth).as("user %d, previous calendar month", userId).isGreaterThanOrEqualTo(4);

            long weeks = all.stream()
                    .filter(a -> !a.getStartTime().isBefore(chartStart))
                    .map(a -> a.getStartTime().toLocalDate().with(DayOfWeek.MONDAY))
                    .distinct()
                    .count();
            assertThat(weeks).as("user %d, distinct weeks in the 12-week chart", userId).isGreaterThanOrEqualTo(10);

            assertThat(all.stream().filter(a -> a.getId() > SEEDED_ACTIVITIES))
                    .as("history rows are plain manual entries without streams")
                    .allSatisfy(a -> assertThat(a.getRoutePolyline()).isNull());
        }
    }

    @Test
    @DisplayName("recalculating personal records over the history still yields exactly the seeded V4 rows")
    void historyDoesNotRewriteSeededRecords() {
        Map<Long, Set<String>> seeded = personalRecordSnapshot();
        assertThat(seeded.values().stream().mapToInt(Set::size).sum()).isEqualTo(17);

        SAMPLE_USERS.forEach(personalRecordService::recalculateAllPRs);

        assertThat(personalRecordSnapshot()).isEqualTo(seeded);
    }

    private Map<Long, Set<String>> personalRecordSnapshot() {
        return SAMPLE_USERS.stream().collect(Collectors.toMap(
                userId -> userId,
                userId -> personalRecordRepository.findByUserId(userId).stream()
                        .map(pr -> pr.getActivityType() + "/" + pr.getDistanceType() + "/" + pr.getTimeInSeconds())
                        .collect(Collectors.toSet())));
    }
}
