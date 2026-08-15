package com.strava.activityservice.activity;

import com.strava.activityservice.domain.Activity;
import com.strava.activityservice.domain.ActivityRepository;
import com.strava.activityservice.domain.ActivityType;
import com.strava.activityservice.event.ActivityEventPublisher;
import com.strava.activityservice.exception.ApiRequestException;
import com.strava.activityservice.pr.PersonalRecordService;
import com.strava.activityservice.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.strava.activityservice.constants.ErrorMessage.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityService Unit Tests")
class ActivityServiceTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private PersonalRecordService personalRecordService;

    @Mock
    private ActivityEventPublisher eventPublisher;

    @InjectMocks
    private ActivityService activityService;

    private Activity testActivity;
    private ActivityRequest activityRequest;

    @BeforeEach
    void setUp() {
        testActivity = TestDataFactory.createActivity(1L, 1L);
        activityRequest = TestDataFactory.createActivityRequest();
    }

    @Nested
    @DisplayName("getActivityById Tests")
    class GetActivityByIdTests {

        @Test
        @DisplayName("Should return activity when found")
        void shouldReturnActivityWhenFound() {
            when(activityRepository.findById(1L)).thenReturn(Optional.of(testActivity));

            Activity result = activityService.getActivityById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("Morning Run");
            verify(activityRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw exception when activity not found")
        void shouldThrowExceptionWhenActivityNotFound() {
            when(activityRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> activityService.getActivityById(999L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(ACTIVITY_NOT_FOUND)
                    .extracting("status")
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("createActivity Tests")
    class CreateActivityTests {

        @Test
        @DisplayName("Should create activity, recompute PRs and publish the created event")
        void shouldCreateActivitySuccessfully() {
            when(activityRepository.save(any(Activity.class))).thenAnswer(invocation -> {
                Activity saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            Activity result = activityService.createActivity(1L, activityRequest);

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("Evening Ride");
            assertThat(result.getActivityType()).isEqualTo(ActivityType.RIDE);
            assertThat(result.getDistance()).isEqualTo(25.0);
            assertThat(result.getUserId()).isEqualTo(1L);
            verify(activityRepository).save(any(Activity.class));
            verify(personalRecordService).checkAndUpdatePRs(result);
            verify(eventPublisher).publishActivityCreated(result);
        }
    }

    @Nested
    @DisplayName("updateActivity Tests")
    class UpdateActivityTests {

        @Test
        @DisplayName("Should update activity successfully when owner")
        void shouldUpdateActivitySuccessfullyWhenOwner() {
            when(activityRepository.findById(1L)).thenReturn(Optional.of(testActivity));
            when(activityRepository.save(any(Activity.class))).thenReturn(testActivity);

            Activity result = activityService.updateActivity(1L, 1L, activityRequest);

            assertThat(result.getTitle()).isEqualTo("Evening Ride");
            assertThat(result.getDistance()).isEqualTo(25.0);
            verify(activityRepository).save(any(Activity.class));
            verify(personalRecordService).checkAndUpdatePRs(testActivity);
            verify(eventPublisher).publishActivityUpdated(testActivity);
            verify(eventPublisher, never()).publishActivityVisibilityChanged(any());
        }

        @Test
        @DisplayName("Should publish a visibility-changed event when isPrivate flips")
        void shouldPublishVisibilityChangedOnPrivacyFlip() {
            testActivity.setPrivate(false);
            activityRequest.setPrivate(true);
            when(activityRepository.findById(1L)).thenReturn(Optional.of(testActivity));
            when(activityRepository.save(any(Activity.class))).thenReturn(testActivity);

            activityService.updateActivity(1L, 1L, activityRequest);

            verify(eventPublisher).publishActivityUpdated(testActivity);
            verify(eventPublisher).publishActivityVisibilityChanged(testActivity);
        }

        @Test
        @DisplayName("Should throw exception when not owner")
        void shouldThrowExceptionWhenNotOwner() {
            when(activityRepository.findById(1L)).thenReturn(Optional.of(testActivity));

            assertThatThrownBy(() -> activityService.updateActivity(1L, 2L, activityRequest))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(ACTIVITY_ACCESS_DENIED)
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("Should throw exception when activity not found")
        void shouldThrowExceptionWhenActivityNotFound() {
            when(activityRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> activityService.updateActivity(999L, 1L, activityRequest))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(ACTIVITY_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("deleteActivity Tests")
    class DeleteActivityTests {

        @Test
        @DisplayName("Should delete activity (with its PRs) and publish the deleted event")
        void shouldDeleteActivitySuccessfullyWhenOwner() {
            when(activityRepository.findById(1L)).thenReturn(Optional.of(testActivity));

            activityService.deleteActivity(1L, 1L);

            verify(personalRecordService).deleteByActivityId(1L);
            verify(activityRepository).delete(testActivity);
            verify(eventPublisher).publishActivityDeleted(1L, 1L);
        }

        @Test
        @DisplayName("Should throw exception when not owner")
        void shouldThrowExceptionWhenNotOwner() {
            when(activityRepository.findById(1L)).thenReturn(Optional.of(testActivity));

            assertThatThrownBy(() -> activityService.deleteActivity(1L, 2L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(ACTIVITY_ACCESS_DENIED);

            verify(activityRepository, never()).delete(any());
            verify(eventPublisher, never()).publishActivityDeleted(any(), any());
        }
    }

    @Nested
    @DisplayName("getMonthlyStats Tests")
    class GetMonthlyStatsTests {

        @Test
        @DisplayName("Should return monthly stats with trend percentages")
        void shouldReturnMonthlyStats() {
            Object[] currentStats = new Object[]{100.0, 36000L, 5000, 1500, 10};
            Object[] prevStats = new Object[]{80.0, 30000L, 4000, 1200, 8};
            when(activityRepository.getAggregatedStatsByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(currentStats)
                    .thenReturn(prevStats);

            ActivityStatsResponse result = activityService.getMonthlyStats(1L);

            assertThat(result.getTotalDistance()).isEqualTo(100.0);
            assertThat(result.getTotalDuration()).isEqualTo(36000L);
            assertThat(result.getTotalCalories()).isEqualTo(5000);
            assertThat(result.getTotalActivities()).isEqualTo(10);
            assertThat(result.getAverageDistance()).isEqualTo(10.0);
            assertThat(result.getDistanceTrendPercentage()).isEqualTo(25.0);
            assertThat(result.getDurationTrendPercentage()).isEqualTo(20.0);
            assertThat(result.getCaloriesTrendPercentage()).isEqualTo(25.0);
            assertThat(result.getActivitiesTrendPercentage()).isEqualTo(25.0);
        }

        @Test
        @DisplayName("Should handle zero activities")
        void shouldHandleZeroActivities() {
            Object[] currentStats = new Object[]{0.0, 0L, 0, 0, 0};
            Object[] prevStats = new Object[]{0.0, 0L, 0, 0, 0};
            when(activityRepository.getAggregatedStatsByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(currentStats)
                    .thenReturn(prevStats);

            ActivityStatsResponse result = activityService.getMonthlyStats(1L);

            assertThat(result.getTotalDistance()).isEqualTo(0.0);
            assertThat(result.getTotalActivities()).isEqualTo(0);
            assertThat(result.getAverageDistance()).isEqualTo(0.0);
            assertThat(result.getDistanceTrendPercentage()).isEqualTo(0.0);
            assertThat(result.getDurationTrendPercentage()).isEqualTo(0.0);
            assertThat(result.getCaloriesTrendPercentage()).isEqualTo(0.0);
            assertThat(result.getActivitiesTrendPercentage()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("getWeeklyStats Tests")
    class GetWeeklyStatsTests {

        @Test
        @DisplayName("Should return weekly stats with daily breakdown")
        void shouldReturnWeeklyStatsWithDailyBreakdown() {
            // Previous week distance for trend calculation
            when(activityRepository.sumDistanceByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(Optional.of(40.0));
            // Daily stats: PostgreSQL DOW 1=Monday with 50km
            List<Object[]> dailyResults = new ArrayList<>();
            dailyResults.add(new Object[]{1, 50.0, 18000, 5});
            when(activityRepository.getDailyStatsByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(dailyResults);

            WeeklyStatsResponse result = activityService.getWeeklyStats(1L);

            assertThat(result.getTotalDistance()).isEqualTo(50.0);
            assertThat(result.getDailyStats()).hasSize(7);
            assertThat(result.getDailyStats().get(0).getDayName()).isEqualTo("Mon");
            assertThat(result.getDailyStats().get(0).getDistance()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("Should map PostgreSQL DOW 0 (Sunday) to the last slot")
        void shouldMapSundayToLastSlot() {
            when(activityRepository.sumDistanceByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(Optional.of(0.0));
            List<Object[]> dailyResults = new ArrayList<>();
            dailyResults.add(new Object[]{0, 12.0, 3600, 1});
            when(activityRepository.getDailyStatsByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(dailyResults);

            WeeklyStatsResponse result = activityService.getWeeklyStats(1L);

            assertThat(result.getDailyStats().get(6).getDayName()).isEqualTo("Sun");
            assertThat(result.getDailyStats().get(6).getDistance()).isEqualTo(12.0);
        }
    }

    @Nested
    @DisplayName("getUserTotalStats Tests")
    class GetUserTotalStatsTests {

        @Test
        @DisplayName("Should return the exact stat keys the frontend reads")
        void shouldReturnTotalStats() {
            when(activityRepository.countByUserId(1L)).thenReturn(10);
            when(activityRepository.sumDistanceByUserId(1L)).thenReturn(150.5);
            when(activityRepository.sumDurationByUserId(1L)).thenReturn(36000L);
            when(activityRepository.sumElevationByUserId(1L)).thenReturn(1200L);
            when(activityRepository.sumCaloriesByUserId(1L)).thenReturn(9000L);

            var stats = activityService.getUserTotalStats(1L);

            assertThat(stats).containsOnlyKeys(
                    "totalActivities", "totalDistanceKm", "totalDurationSeconds",
                    "totalElevationMeters", "totalCalories");
            assertThat(stats.get("totalActivities")).isEqualTo(10);
            assertThat(stats.get("totalDistanceKm")).isEqualTo(150.5);
            assertThat(stats.get("totalDurationSeconds")).isEqualTo(36000L);
        }
    }

    @Nested
    @DisplayName("Photo upload rules")
    class PhotoUploadRules {

        @Test
        @DisplayName("rejects uploads past the 5-photo limit")
        void rejectsUploadsPastLimit() {
            testActivity.setPhotos(new ArrayList<>(List.of("/a", "/b", "/c", "/d", "/e")));
            when(activityRepository.findById(1L)).thenReturn(Optional.of(testActivity));

            var file = new MockMultipartFile("files", "a.jpg", "image/jpeg", new byte[]{1});
            assertThatThrownBy(() -> activityService.uploadPhotos(1L, 1L, List.of(file)))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(PHOTO_LIMIT_EXCEEDED);
        }

        @Test
        @DisplayName("rejects disallowed content types")
        void rejectsDisallowedContentTypes() {
            when(activityRepository.findById(1L)).thenReturn(Optional.of(testActivity));

            var file = new MockMultipartFile("files", "a.gif", "image/gif", new byte[]{1});
            assertThatThrownBy(() -> activityService.uploadPhotos(1L, 1L, List.of(file)))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(INVALID_PHOTO_TYPE);
        }

        @Test
        @DisplayName("rejects photo deletion with an out-of-range index")
        void rejectsInvalidPhotoIndex() {
            testActivity.setPhotos(new ArrayList<>(List.of("/uploads/photos/a.jpg")));
            when(activityRepository.findById(1L)).thenReturn(Optional.of(testActivity));

            assertThatThrownBy(() -> activityService.deletePhoto(1L, 1L, 3))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(INVALID_PHOTO_INDEX);
        }

        @Test
        @DisplayName("only the owner may manage photos")
        void onlyOwnerMayManagePhotos() {
            when(activityRepository.findById(1L)).thenReturn(Optional.of(testActivity));

            var file = new MockMultipartFile("files", "a.jpg", "image/jpeg", new byte[]{1});
            assertThatThrownBy(() -> activityService.uploadPhotos(1L, 99L, List.of(file)))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(ACTIVITY_ACCESS_DENIED);
        }
    }

    @Nested
    @DisplayName("GPS stream alignment validation")
    class StreamAlignmentValidation {

        /// Straight-line JSON polyline with 3 points.
        private static final String ROUTE = "[[52.0,21.0],[52.001,21.0],[52.002,21.0]]";

        private ActivityRequest requestWithStreams(String route, String timestamps, String elevation) {
            ActivityRequest request = TestDataFactory.createActivityRequest();
            request.setRoutePolyline(route);
            request.setPointTimestamps(timestamps);
            request.setElevationProfile(elevation);
            return request;
        }

        @Test
        @DisplayName("aligned streams pass and are persisted")
        void alignedStreamsPersist() {
            when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));

            Activity saved = activityService.createActivity(
                    1L, requestWithStreams(ROUTE, "[100,160,220]", "[10.0,12.5,11.0]"));

            assertThat(saved.getPointTimestamps()).isEqualTo("[100,160,220]");
            assertThat(saved.getElevationProfile()).isEqualTo("[10.0,12.5,11.0]");
        }

        @Test
        @DisplayName("timestamps whose length does not match the route are rejected")
        void misalignedTimestampsRejected() {
            assertThatThrownBy(() -> activityService.createActivity(
                    1L, requestWithStreams(ROUTE, "[100,160]", null)))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessageContaining("pointTimestamps");
        }

        @Test
        @DisplayName("decreasing timestamps are rejected")
        void decreasingTimestampsRejected() {
            assertThatThrownBy(() -> activityService.createActivity(
                    1L, requestWithStreams(ROUTE, "[220,160,100]", null)))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessageContaining("pointTimestamps");
        }

        @Test
        @DisplayName("misaligned elevation profiles are rejected")
        void misalignedElevationRejected() {
            assertThatThrownBy(() -> activityService.createActivity(
                    1L, requestWithStreams(ROUTE, null, "[10.0]")))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessageContaining("elevationProfile");
        }

        @Test
        @DisplayName("streams without a usable route are rejected")
        void streamsWithoutRouteRejected() {
            assertThatThrownBy(() -> activityService.createActivity(
                    1L, requestWithStreams(null, "[100,160,220]", null)))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessageContaining("route polyline");
        }

        @Test
        @DisplayName("updating the route replaces its streams instead of keeping stale ones")
        void updateReplacesStreamsWithTheRoute() {
            testActivity.setRoutePolyline("[[50.0,20.0],[50.001,20.0]]");
            testActivity.setPointTimestamps("[1,2]");
            testActivity.setElevationProfile("[5.0,6.0]");
            when(activityRepository.findById(1L)).thenReturn(Optional.of(testActivity));
            when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));

            ActivityRequest request = requestWithStreams(ROUTE, "[100,160,220]", null);
            Activity saved = activityService.updateActivity(1L, 1L, request);

            assertThat(saved.getRoutePolyline()).isEqualTo(ROUTE);
            assertThat(saved.getPointTimestamps()).isEqualTo("[100,160,220]");
            assertThat(saved.getElevationProfile()).isNull();
        }
    }
}
