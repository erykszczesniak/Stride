package com.strava.pipeline.pipeline;

import com.strava.pipeline.client.ActivityClient;
import com.strava.pipeline.client.EventClient;
import com.strava.pipeline.client.GoalClient;
import com.strava.pipeline.client.UserClient;
import com.strava.pipeline.exception.ApiRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Adapted from main: the repository/domain-service mocks became client mocks
 * (REST composition), the math components stay real on purpose. The
 * cross-user visibility rules main asserted here are enforced downstream by
 * activity-service's ActivityVisibility (unit-tested there); this facade's
 * contract is that a downstream 404 — unknown id or visibility denial —
 * surfaces as the pipeline's not_found.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PipelineQueryService")
class PipelineQueryServiceTest {

    private static final long USER_ID = 7L;

    @Mock
    private ActivityClient activityClient;

    @Mock
    private UserClient userClient;

    @Mock
    private GoalClient goalClient;

    @Mock
    private EventClient eventClient;

    private PipelineQueryService service;

    @BeforeEach
    void setUp() {
        // Real math components on purpose — only data access is mocked.
        service = new PipelineQueryService(
                activityClient,
                userClient,
                goalClient,
                eventClient,
                new ActivityStreamAnalyzer(),
                new TrainingLoadCalculator());
    }

    private ActivityData activity(long id, ActivityType type, double distanceKm, int durationSeconds,
                                  LocalDateTime startTime) {
        return ActivityData.builder()
                .id(id)
                .userId(USER_ID)
                .title("Activity " + id)
                .activityType(type)
                .distance(distanceKm)
                .duration(durationSeconds)
                .startTime(startTime)
                .build();
    }

    @Test
    @DisplayName("maps activities to the contract and filters non-triathlon disciplines")
    void listActivitiesFiltersAndMaps() {
        ActivityData run = activity(1, ActivityType.RUN, 5.0, 1500, LocalDateTime.now().minusDays(1));
        ActivityData walk = activity(2, ActivityType.WALK, 5.0, 3600, LocalDateTime.now().minusDays(2));
        ActivityData ride = activity(3, ActivityType.RIDE, 40.0, 5400, LocalDateTime.now());
        given(activityClient.myActivities(USER_ID)).willReturn(List.of(run, walk, ride));

        List<PipelineActivityDto> result = service.listActivities(USER_ID);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo("act-3"); // newest first
        assertThat(result.get(0).discipline()).isEqualTo("bike");
        assertThat(result.get(1).discipline()).isEqualTo("run");
        // Contract requires non-optional ints; missing HR falls back to 0.
        assertThat(result.get(0).avgHeartRate()).isZero();
        assertThat(result.get(0).startDate().getOffset()).isNotNull();
        assertThat(result.get(0).hasGPS()).isFalse();
    }

    @Test
    @DisplayName("activity detail serves activities activity-service deems viewable (feed tap-through)")
    void detailServesViewableForeignActivity() {
        ActivityData foreign = ActivityData.builder()
                .id(9L)
                .userId(999L)
                .title("Foreign run")
                .activityType(ActivityType.RUN)
                .distance(5.0)
                .duration(1500)
                .startTime(LocalDateTime.now())
                .build();
        given(activityClient.activityDetail(USER_ID, 9L)).willReturn(foreign);

        PipelineActivityDetailDto detail = service.activityDetail(USER_ID, "act-9");

        assertThat(detail.activity().id()).isEqualTo("act-9");
    }

    @Test
    @DisplayName("activity detail propagates the downstream 404 (unknown id or visibility denial)")
    void detailPropagatesDownstreamNotFound() {
        given(activityClient.activityDetail(USER_ID, 9L))
                .willThrow(new ApiRequestException("Activity not found", HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> service.activityDetail(USER_ID, "act-9"))
                .isInstanceOf(ApiRequestException.class)
                .hasMessage("Activity not found");
    }

    @Test
    @DisplayName("activity detail hides non-triathlon disciplines behind 404")
    void detailFiltersNonTriDisciplines() {
        given(activityClient.activityDetail(USER_ID, 4L))
                .willReturn(activity(4, ActivityType.WALK, 5.0, 3600, LocalDateTime.now()));

        assertThatThrownBy(() -> service.activityDetail(USER_ID, "act-4"))
                .isInstanceOf(ApiRequestException.class);
    }

    @Test
    @DisplayName("activity detail rejects malformed ids as 404, not 500, without a downstream call")
    void detailRejectsMalformedIds() {
        assertThatThrownBy(() -> service.activityDetail(USER_ID, "act-oops"))
                .isInstanceOf(ApiRequestException.class);
        verifyNoInteractions(activityClient);
    }

    @Test
    @DisplayName("activity detail carries splits and 17-sample series")
    void detailBuildsSeriesAndSplits() {
        ActivityData run = ActivityData.builder()
                .id(1L)
                .userId(USER_ID)
                .title("Activity 1")
                .activityType(ActivityType.RUN)
                .distance(5.0)
                .duration(1500)
                .averageHeartRate(150)
                .maxHeartRate(165)
                .startTime(LocalDateTime.now())
                .build();
        given(activityClient.activityDetail(USER_ID, 1L)).willReturn(run);

        PipelineActivityDetailDto detail = service.activityDetail(USER_ID, "act-1");

        assertThat(detail.activity().id()).isEqualTo("act-1");
        assertThat(detail.paceSeries()).hasSize(ActivityStreamAnalyzer.SERIES_SAMPLES);
        assertThat(detail.heartRateSeries()).hasSize(ActivityStreamAnalyzer.SERIES_SAMPLES);
        assertThat(detail.splits()).hasSize(5);
        assertThat(detail.splits().get(4).distanceMeters()).isEqualTo(5000);
        assertThat(detail.splits().get(0).avgHeartRate()).isBetween(128, 165);
        // No stored route/elevation → explicit null/empty, never absent fields.
        assertThat(detail.routePolyline()).isNull();
        assertThat(detail.elevationSeries()).isEmpty();
    }

    @Test
    @DisplayName("dashboard snapshots cover all three disciplines with week-over-week trends")
    void dashboardSnapshots() {
        LocalDateTime thisWeek = LocalDate.now().with(DayOfWeek.MONDAY).atTime(8, 0);
        LocalDateTime lastWeek = thisWeek.minusWeeks(1);
        ActivityData runNow = activity(1, ActivityType.RUN, 5.0, 1500, thisWeek);
        ActivityData runPrev = activity(2, ActivityType.RUN, 10.0, 3000, lastWeek);
        ActivityData rideNow = activity(3, ActivityType.RIDE, 10.0, 1800, thisWeek);
        given(activityClient.myActivities(USER_ID)).willReturn(List.of(runNow, runPrev, rideNow));

        PipelineDashboardDto dashboard = service.dashboard(USER_ID);

        assertThat(dashboard.todayWorkout()).isNull();
        assertThat(dashboard.snapshots()).extracting(PipelineDashboardDto.SportSnapshot::discipline)
                .containsExactly("swim", "bike", "run");
        PipelineDashboardDto.SportSnapshot run = dashboard.snapshots().get(2);
        assertThat(run.weekDistanceKm()).isEqualTo(5.0);
        assertThat(run.trendPercent()).isEqualTo(-50);
        PipelineDashboardDto.SportSnapshot bike = dashboard.snapshots().get(1);
        assertThat(bike.trendPercent()).isEqualTo(100); // nothing last week
        assertThat(dashboard.recentActivities()).hasSize(3);
        assertThat(dashboard.form().ctl()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("records map to the tagged-union value shape with labels and progression")
    void recordsMapping() {
        RecordData fiveK = RecordData.builder()
                .id(11L)
                .activityType(ActivityType.RUN)
                .distanceType(DistanceType.FIVE_KM)
                .timeInSeconds(1500)
                .previousTimeInSeconds(1694)
                .achievedAt(LocalDateTime.now().minusDays(2))
                .build();
        RecordData swim = RecordData.builder()
                .id(12L)
                .activityType(ActivityType.SWIM)
                .distanceType(DistanceType.ONE_KM)
                .timeInSeconds(1400)
                .achievedAt(LocalDateTime.now().minusDays(60))
                .build();
        RecordData walk = RecordData.builder()
                .id(13L)
                .activityType(ActivityType.WALK)
                .distanceType(DistanceType.FIVE_KM)
                .timeInSeconds(3600)
                .achievedAt(LocalDateTime.now())
                .build();
        given(activityClient.personalRecords(USER_ID)).willReturn(List.of(swim, fiveK, walk));

        PipelineRecordsDto records = service.records(USER_ID);

        assertThat(records.records()).hasSize(2); // WALK is not a pipeline discipline
        PipelineRecordsDto.RecordItem first = records.records().get(0);
        assertThat(first.discipline()).isEqualTo("run"); // run sorts before swim
        assertThat(first.eventLabel()).isEqualTo("5K");
        assertThat(first.value().time().seconds()).isEqualTo(1500);
        assertThat(first.isNew()).isTrue();
        assertThat(first.previousBest().time().seconds()).isEqualTo(1694);
        assertThat(first.progression()).containsExactly(1694.0, 1500.0);
        PipelineRecordsDto.RecordItem second = records.records().get(1);
        assertThat(second.isNew()).isFalse();
        assertThat(second.previousBest()).isNull();
        assertThat(second.progression()).containsExactly(1400.0);
    }

    @Test
    @DisplayName("profile fills the non-optional contract fields from available data")
    void profileFallbacks() {
        given(userClient.me(USER_ID)).willReturn(new UserClient.UserData("Eryk", "Szczesniak", null));
        given(activityClient.myActivities(USER_ID)).willReturn(List.of());
        given(activityClient.personalRecords(USER_ID)).willReturn(List.of());
        given(eventClient.myUpcomingEvents(USER_ID)).willReturn(List.of());
        given(goalClient.activeGoals(USER_ID)).willReturn(List.of());

        var profile = service.profile(USER_ID);

        assertThat(profile.athlete().name()).isEqualTo("Eryk Szczesniak");
        assertThat(profile.athlete().initials()).isEqualTo("ES");
        assertThat(profile.athlete().ageGroup()).isNotBlank();
        assertThat(profile.seasonStats().sessions()).isZero();
        assertThat(profile.seasonGoal().title()).isNotBlank();
        assertThat(profile.seasonGoal().date()).isNotNull();
        assertThat(profile.thresholdZones()).isNotNull();
    }

    @Test
    @DisplayName("profile derives the season goal from the next event and the first TIME goal")
    void profileSeasonGoalFromEventAndTimeGoal() {
        LocalDateTime raceDay = LocalDateTime.now().plusDays(40);
        given(userClient.me(USER_ID)).willReturn(new UserClient.UserData("Eryk", "Szczesniak", "Warsaw"));
        given(activityClient.myActivities(USER_ID)).willReturn(List.of());
        given(activityClient.personalRecords(USER_ID)).willReturn(List.of());
        given(eventClient.myUpcomingEvents(USER_ID))
                .willReturn(List.of(new EventClient.EventData("Ironman Copenhagen", raceDay)));
        given(goalClient.activeGoals(USER_ID)).willReturn(List.of(
                new GoalClient.GoalData("DISTANCE", 500.0),
                new GoalClient.GoalData("TIME", 12.0)));

        var profile = service.profile(USER_ID);

        assertThat(profile.athlete().city()).isEqualTo("Warsaw");
        assertThat(profile.seasonGoal().title()).isEqualTo("Ironman Copenhagen");
        assertThat(profile.seasonGoal().targetSeconds()).isEqualTo(12 * 3600);
    }

    @Test
    @DisplayName("profile FTP comes from the internal power enrichment (95% of best ride power)")
    void profileFtpFromPowerEnrichment() {
        ActivityData ride = activity(3, ActivityType.RIDE, 40.0, 5400, LocalDateTime.now());
        given(userClient.me(USER_ID)).willReturn(new UserClient.UserData("Eryk", "Szczesniak", null));
        given(activityClient.myActivities(USER_ID)).willReturn(List.of(ride));
        given(activityClient.personalRecords(USER_ID)).willReturn(List.of());
        given(eventClient.myUpcomingEvents(USER_ID)).willReturn(List.of());
        given(goalClient.activeGoals(USER_ID)).willReturn(List.of());
        given(activityClient.averagePowerByIds(List.of(3L))).willReturn(Map.of(3L, 200));

        var profile = service.profile(USER_ID);

        assertThat(profile.thresholdZones().ftpWatts()).isEqualTo(190);
    }
}
