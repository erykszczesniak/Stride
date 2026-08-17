package com.strava.activityservice.grpc;

import com.strava.activityservice.activity.ActivityService;
import com.strava.activityservice.domain.Activity;
import com.strava.activityservice.domain.ActivityRepository;
import com.strava.activityservice.domain.ActivityType;
import com.strava.activityservice.internal.InternalActivityQueryService;
import com.strava.activityservice.util.TestDataFactory;
import com.strava.grpc.activity.ActivityPayload;
import com.strava.grpc.activity.ActivityStatsRange;
import com.strava.grpc.activity.GetActivitiesRequest;
import com.strava.grpc.activity.GetActivitiesResponse;
import com.strava.grpc.activity.GetStatsRangeRequest;
import com.strava.grpc.activity.GetUserActivitySummaryRequest;
import com.strava.grpc.activity.UserActivitySummary;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Direct-invocation unit tests for the gRPC transport (no server, no Spring
 * context): methods are called with a capturing StreamObserver. The real
 * InternalActivityQueryService sits underneath with mocked repository /
 * ActivityService, mirroring the fixtures of InternalActivityControllerTest
 * so both transports are held to the same expectations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityInternalGrpcService Unit Tests")
class ActivityInternalGrpcServiceTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private ActivityService activityService;

    private ActivityInternalGrpcService grpcService;

    @BeforeEach
    void setUp() {
        grpcService = new ActivityInternalGrpcService(
                new InternalActivityQueryService(activityRepository, activityService));
    }

    /** Minimal StreamObserver that records emissions for assertions. */
    private static final class CapturingObserver<T> implements StreamObserver<T> {
        private final List<T> values = new ArrayList<>();
        private Throwable error;
        private boolean completed;

        @Override
        public void onNext(T value) {
            values.add(value);
        }

        @Override
        public void onError(Throwable t) {
            error = t;
        }

        @Override
        public void onCompleted() {
            completed = true;
        }

        T single() {
            assertThat(error).isNull();
            assertThat(completed).isTrue();
            assertThat(values).hasSize(1);
            return values.get(0);
        }

        StatusRuntimeException statusError() {
            assertThat(completed).isFalse();
            assertThat(values).isEmpty();
            assertThat(error).isInstanceOf(StatusRuntimeException.class);
            return (StatusRuntimeException) error;
        }
    }

    @Test
    @DisplayName("GetActivities: batch payload carries the contract fields; unknown ids are silently skipped")
    void batchFetchMapsFieldsAndSkipsUnknownIds() {
        Activity activity = TestDataFactory.createActivity(1L, 5L);
        activity.setPrivate(true);
        activity.setRoutePolyline("abc");
        activity.setElevationProfile("[1.0,2.0]");
        activity.setPointTimestamps("[100,200]");
        activity.setAverageHeartRate(150);
        activity.setAveragePower(210);
        activity.setCalories(600);
        activity.setStartLocation("Warsaw");
        activity.setPhotos(List.of("/uploads/photos/a.jpg"));
        when(activityRepository.findAllById(List.of(1L, 999L))).thenReturn(List.of(activity));

        CapturingObserver<GetActivitiesResponse> observer = new CapturingObserver<>();
        grpcService.getActivities(
                GetActivitiesRequest.newBuilder().addIds(1L).addIds(999L).build(), observer);

        GetActivitiesResponse response = observer.single();
        assertThat(response.getActivitiesCount()).isEqualTo(1); // id 999 silently skipped

        ActivityPayload p = response.getActivities(0);
        assertThat(p.getId()).isEqualTo(1L);
        assertThat(p.getUserId()).isEqualTo(5L);
        assertThat(p.getIsPrivate()).isTrue();
        assertThat(p.getTitle()).isEqualTo("Morning Run");
        assertThat(p.getActivityType()).isEqualTo("RUN");
        assertThat(p.getRoutePolyline()).isEqualTo("abc");
        assertThat(p.getElevationProfileList()).containsExactly(1.0, 2.0);
        assertThat(p.getPointTimestampsList()).containsExactly(100L, 200L);
        assertThat(p.getAverageHeartRate()).isEqualTo(150);
        assertThat(p.getAveragePower()).isEqualTo(210);
        assertThat(p.getCalories()).isEqualTo(600);
        assertThat(p.getStartLocation()).isEqualTo("Warsaw");
        assertThat(p.getPhotosList()).containsExactly("/uploads/photos/a.jpg");
        assertThat(p.getDistance()).isEqualTo(10.0);
        assertThat(p.getDuration()).isEqualTo(3600);
    }

    @Test
    @DisplayName("GetActivities: timestamps serialize as ISO-8601 local date-times")
    void timestampsAreIso8601() {
        Activity activity = TestDataFactory.createActivity(1L, 5L);
        activity.setStartTime(LocalDateTime.of(2026, 3, 1, 7, 30));
        activity.setEndTime(LocalDateTime.of(2026, 3, 1, 8, 30, 15));
        activity.setCreatedAt(LocalDateTime.of(2026, 3, 1, 9, 0));
        when(activityRepository.findAllById(anyList())).thenReturn(List.of(activity));

        CapturingObserver<GetActivitiesResponse> observer = new CapturingObserver<>();
        grpcService.getActivities(GetActivitiesRequest.newBuilder().addIds(1L).build(), observer);

        ActivityPayload p = observer.single().getActivities(0);
        assertThat(p.getStartTime()).isEqualTo("2026-03-01T07:30");
        assertThat(p.getEndTime()).isEqualTo("2026-03-01T08:30:15");
        assertThat(p.getCreatedAt()).isEqualTo("2026-03-01T09:00");
        assertThat(p.hasUpdatedAt()).isFalse();
    }

    @Test
    @DisplayName("GetActivities: null metrics and streams are absent, not zero-defaulted")
    void optionalFieldsAbsentWhenNull() {
        Activity bare = Activity.builder()
                .id(2L)
                .userId(5L)
                .title("Bare swim")
                .activityType(ActivityType.SWIM)
                .distance(1.0)
                .duration(1800)
                .startTime(LocalDateTime.of(2026, 3, 1, 7, 0))
                .build();
        when(activityRepository.findAllById(anyList())).thenReturn(List.of(bare));

        CapturingObserver<GetActivitiesResponse> observer = new CapturingObserver<>();
        grpcService.getActivities(GetActivitiesRequest.newBuilder().addIds(2L).build(), observer);

        ActivityPayload p = observer.single().getActivities(0);
        assertThat(p.hasDescription()).isFalse();
        assertThat(p.hasElevationGain()).isFalse();
        assertThat(p.hasCalories()).isFalse();
        assertThat(p.hasAverageSpeed()).isFalse();
        assertThat(p.hasMaxSpeed()).isFalse();
        assertThat(p.hasAverageHeartRate()).isFalse();
        assertThat(p.hasMaxHeartRate()).isFalse();
        assertThat(p.hasAverageCadence()).isFalse();
        assertThat(p.hasAveragePower()).isFalse();
        assertThat(p.hasEndTime()).isFalse();
        assertThat(p.hasStartLocation()).isFalse();
        assertThat(p.hasEndLocation()).isFalse();
        assertThat(p.hasRoutePolyline()).isFalse();
        assertThat(p.hasMapImageUrl()).isFalse();
        assertThat(p.hasCreatedAt()).isFalse();
        assertThat(p.hasUpdatedAt()).isFalse();
        assertThat(p.getElevationProfileCount()).isZero();
        assertThat(p.getPointTimestampsCount()).isZero();
        assertThat(p.getPhotosCount()).isZero();
        // required-by-schema fields are still present
        assertThat(p.hasDistance()).isTrue();
        assertThat(p.hasDuration()).isTrue();
        assertThat(p.hasStartTime()).isTrue();
    }

    @Test
    @DisplayName("GetStatsRange: maps the aggregation result onto the wire shape")
    void statsRangeMapsAggregates() {
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 1, 31, 23, 59, 59);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalDistanceKm", 120.5);
        stats.put("totalDurationSeconds", 36000L);
        stats.put("totalElevationGain", 900L);
        stats.put("totalCalories", 8000L);
        stats.put("activityCount", 12);
        when(activityService.getRangeStats(7L, from, to)).thenReturn(stats);

        CapturingObserver<ActivityStatsRange> observer = new CapturingObserver<>();
        grpcService.getStatsRange(GetStatsRangeRequest.newBuilder()
                .setUserId(7L)
                .setFrom("2026-01-01T00:00:00")
                .setTo("2026-01-31T23:59:59")
                .build(), observer);

        ActivityStatsRange range = observer.single();
        assertThat(range.getTotalDistanceKm()).isEqualTo(120.5);
        assertThat(range.getTotalDurationSeconds()).isEqualTo(36000L);
        assertThat(range.getTotalElevationGain()).isEqualTo(900L);
        assertThat(range.getTotalCalories()).isEqualTo(8000L);
        assertThat(range.getActivityCount()).isEqualTo(12);
    }

    @Test
    @DisplayName("GetStatsRange: unparseable date -> INVALID_ARGUMENT, no data access")
    void statsRangeRejectsBadDates() {
        CapturingObserver<ActivityStatsRange> observer = new CapturingObserver<>();
        grpcService.getStatsRange(GetStatsRangeRequest.newBuilder()
                .setUserId(7L)
                .setFrom("not-a-date")
                .setTo("2026-01-31T23:59:59")
                .build(), observer);

        StatusRuntimeException error = observer.statusError();
        assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(error.getStatus().getDescription()).contains("from");
        verifyNoInteractions(activityService);
        verifyNoInteractions(activityRepository);
    }

    @Test
    @DisplayName("GetUserActivitySummary without sportFilter: sums the whole startTime window, null metrics count as 0")
    void summaryWithoutSportFilter() {
        LocalDateTime from = LocalDateTime.of(2026, 2, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 2, 28, 23, 59, 59);
        Activity run = Activity.builder().id(1L).userId(9L).title("Run").activityType(ActivityType.RUN)
                .distance(10.0).duration(3600).elevationGain(120)
                .startTime(LocalDateTime.of(2026, 2, 5, 8, 0)).build();
        Activity ride = Activity.builder().id(2L).userId(9L).title("Ride").activityType(ActivityType.RIDE)
                .distance(40.0).duration(7200) // elevationGain null -> contributes 0
                .startTime(LocalDateTime.of(2026, 2, 10, 8, 0)).build();
        Activity bare = Activity.builder().id(3L).userId(9L).title("Swim").activityType(ActivityType.SWIM)
                // all metrics null -> still counted
                .startTime(LocalDateTime.of(2026, 2, 15, 8, 0)).build();
        when(activityRepository.findByUserIdAndStartTimeBetween(9L, from, to))
                .thenReturn(List.of(run, ride, bare));

        CapturingObserver<UserActivitySummary> observer = new CapturingObserver<>();
        grpcService.getUserActivitySummary(GetUserActivitySummaryRequest.newBuilder()
                .setUserId(9L)
                .setFrom("2026-02-01T00:00:00")
                .setTo("2026-02-28T23:59:59")
                .build(), observer);

        UserActivitySummary summary = observer.single();
        assertThat(summary.getTotalDistanceKm()).isEqualTo(50.0);
        assertThat(summary.getTotalDurationSeconds()).isEqualTo(10800L);
        assertThat(summary.getTotalElevationMeters()).isEqualTo(120.0);
        assertThat(summary.getActivityCount()).isEqualTo(3);
        // the window is queried on startTime with inclusive bounds
        verify(activityRepository).findByUserIdAndStartTimeBetween(9L, from, to);
    }

    @Test
    @DisplayName("GetUserActivitySummary with sportFilter: case-insensitive ActivityType match")
    void summaryWithSportFilter() {
        LocalDateTime from = LocalDateTime.of(2026, 2, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 2, 28, 23, 59, 59);
        Activity run = Activity.builder().id(1L).userId(9L).title("Run").activityType(ActivityType.RUN)
                .distance(10.0).duration(3600).elevationGain(120)
                .startTime(LocalDateTime.of(2026, 2, 5, 8, 0)).build();
        Activity ride = Activity.builder().id(2L).userId(9L).title("Ride").activityType(ActivityType.RIDE)
                .distance(40.0).duration(7200).elevationGain(500)
                .startTime(LocalDateTime.of(2026, 2, 10, 8, 0)).build();
        when(activityRepository.findByUserIdAndStartTimeBetween(9L, from, to))
                .thenReturn(List.of(run, ride));

        CapturingObserver<UserActivitySummary> observer = new CapturingObserver<>();
        grpcService.getUserActivitySummary(GetUserActivitySummaryRequest.newBuilder()
                .setUserId(9L)
                .setSportFilter("run") // lowercase on purpose — the old client matched ignore-case
                .setFrom("2026-02-01T00:00:00")
                .setTo("2026-02-28T23:59:59")
                .build(), observer);

        UserActivitySummary summary = observer.single();
        assertThat(summary.getTotalDistanceKm()).isEqualTo(10.0);
        assertThat(summary.getTotalDurationSeconds()).isEqualTo(3600L);
        assertThat(summary.getTotalElevationMeters()).isEqualTo(120.0);
        assertThat(summary.getActivityCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("GetUserActivitySummary: unparseable date -> INVALID_ARGUMENT")
    void summaryRejectsBadDates() {
        CapturingObserver<UserActivitySummary> observer = new CapturingObserver<>();
        grpcService.getUserActivitySummary(GetUserActivitySummaryRequest.newBuilder()
                .setUserId(9L)
                .setFrom("2026-02-01T00:00:00")
                .setTo("2026-02-31T00:00:00") // Feb 31 does not exist
                .build(), observer);

        StatusRuntimeException error = observer.statusError();
        assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(error.getStatus().getDescription()).contains("to");
        verifyNoInteractions(activityRepository);
    }

    @Test
    @DisplayName("unexpected failure -> INTERNAL without leaking the exception message")
    void unexpectedFailureMapsToOpaqueInternal() {
        when(activityRepository.findAllById(anyList()))
                .thenThrow(new RuntimeException("boom: jdbc://stride_activity secret"));

        CapturingObserver<GetActivitiesResponse> observer = new CapturingObserver<>();
        grpcService.getActivities(GetActivitiesRequest.newBuilder().addIds(1L).build(), observer);

        StatusRuntimeException error = observer.statusError();
        assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.INTERNAL);
        assertThat(error.getStatus().getDescription())
                .isEqualTo("Internal error")
                .doesNotContain("boom")
                .doesNotContain("secret");
    }
}
