package com.strava.backend.service.pipeline;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.PersonalRecord;
import com.strava.backend.domain.User;
import com.strava.backend.dto.pipeline.PipelineActivityDetailDto;
import com.strava.backend.dto.pipeline.PipelineActivityDto;
import com.strava.backend.dto.pipeline.PipelineDashboardDto;
import com.strava.backend.dto.pipeline.PipelineRecordsDto;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.enums.DistanceType;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.repository.PersonalRecordRepository;
import com.strava.backend.repository.UserRepository;
import com.strava.backend.service.EventService;
import com.strava.backend.service.GoalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("PipelineQueryService")
class PipelineQueryServiceTest {

    private static final long USER_ID = 7L;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private PersonalRecordRepository personalRecordRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventService eventService;

    @Mock
    private GoalService goalService;

    private PipelineQueryService service;

    private final User owner = User.builder().id(USER_ID).firstName("Eryk").lastName("Szczesniak").build();

    @BeforeEach
    void setUp() {
        // Real math components on purpose — only data access is mocked.
        service = new PipelineQueryService(
                activityRepository,
                personalRecordRepository,
                userRepository,
                eventService,
                goalService,
                new ActivityStreamAnalyzer(),
                new TrainingLoadCalculator());
    }

    private Activity activity(long id, ActivityType type, double distanceKm, int durationSeconds,
                              LocalDateTime startTime) {
        return Activity.builder()
                .id(id)
                .title("Activity " + id)
                .activityType(type)
                .distance(distanceKm)
                .duration(durationSeconds)
                .startTime(startTime)
                .user(owner)
                .build();
    }

    @Test
    @DisplayName("maps activities to the contract and filters non-triathlon disciplines")
    void listActivitiesFiltersAndMaps() {
        Activity run = activity(1, ActivityType.RUN, 5.0, 1500, LocalDateTime.now().minusDays(1));
        Activity walk = activity(2, ActivityType.WALK, 5.0, 3600, LocalDateTime.now().minusDays(2));
        Activity ride = activity(3, ActivityType.RIDE, 40.0, 5400, LocalDateTime.now());
        given(activityRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .willReturn(List.of(run, walk, ride));

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
    @DisplayName("activity detail refuses ids the token owner does not own")
    void detailChecksOwnership() {
        Activity foreign = activity(9, ActivityType.RUN, 5.0, 1500, LocalDateTime.now());
        foreign.setUser(User.builder().id(999L).build());
        given(activityRepository.findById(9L)).willReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.activityDetail(USER_ID, "act-9"))
                .isInstanceOf(ApiRequestException.class);
    }

    @Test
    @DisplayName("activity detail rejects malformed ids as 404, not 500")
    void detailRejectsMalformedIds() {
        assertThatThrownBy(() -> service.activityDetail(USER_ID, "act-oops"))
                .isInstanceOf(ApiRequestException.class);
    }

    @Test
    @DisplayName("activity detail carries splits and 17-sample series")
    void detailBuildsSeriesAndSplits() {
        Activity run = activity(1, ActivityType.RUN, 5.0, 1500, LocalDateTime.now());
        run.setAverageHeartRate(150);
        run.setMaxHeartRate(165);
        given(activityRepository.findById(1L)).willReturn(Optional.of(run));

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
        Activity runNow = activity(1, ActivityType.RUN, 5.0, 1500, thisWeek);
        Activity runPrev = activity(2, ActivityType.RUN, 10.0, 3000, lastWeek);
        Activity rideNow = activity(3, ActivityType.RIDE, 10.0, 1800, thisWeek);
        given(activityRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .willReturn(List.of(runNow, runPrev, rideNow));

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
        PersonalRecord fiveK = PersonalRecord.builder()
                .id(11L)
                .user(owner)
                .activityType(ActivityType.RUN)
                .distanceType(DistanceType.FIVE_KM)
                .timeInSeconds(1500)
                .previousTimeInSeconds(1694)
                .achievedAt(LocalDateTime.now().minusDays(2))
                .build();
        PersonalRecord swim = PersonalRecord.builder()
                .id(12L)
                .user(owner)
                .activityType(ActivityType.SWIM)
                .distanceType(DistanceType.ONE_KM)
                .timeInSeconds(1400)
                .achievedAt(LocalDateTime.now().minusDays(60))
                .build();
        PersonalRecord walk = PersonalRecord.builder()
                .id(13L)
                .user(owner)
                .activityType(ActivityType.WALK)
                .distanceType(DistanceType.FIVE_KM)
                .timeInSeconds(3600)
                .achievedAt(LocalDateTime.now())
                .build();
        given(personalRecordRepository.findByUserId(USER_ID)).willReturn(List.of(swim, fiveK, walk));

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
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(owner));
        given(activityRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).willReturn(List.of());
        given(personalRecordRepository.findByUserId(USER_ID)).willReturn(List.of());
        given(eventService.getUpcomingEventsByUserId(USER_ID, 1)).willReturn(List.of());
        given(goalService.getActiveGoalsByUserId(USER_ID)).willReturn(List.of());

        var profile = service.profile(USER_ID);

        assertThat(profile.athlete().name()).isEqualTo("Eryk Szczesniak");
        assertThat(profile.athlete().initials()).isEqualTo("ES");
        assertThat(profile.athlete().ageGroup()).isNotBlank();
        assertThat(profile.seasonStats().sessions()).isZero();
        assertThat(profile.seasonGoal().title()).isNotBlank();
        assertThat(profile.seasonGoal().date()).isNotNull();
        assertThat(profile.thresholdZones()).isNotNull();
    }
}
