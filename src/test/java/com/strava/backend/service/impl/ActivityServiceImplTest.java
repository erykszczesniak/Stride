package com.strava.backend.service.impl;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.User;
import com.strava.backend.dto.activity.ActivityRequest;
import com.strava.backend.dto.activity.ActivityStatsResponse;
import com.strava.backend.dto.activity.WeeklyStatsResponse;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.service.UserService;
import com.strava.backend.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.strava.backend.constants.ErrorMessage.ACTIVITY_ACCESS_DENIED;
import static com.strava.backend.constants.ErrorMessage.ACTIVITY_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityServiceImpl Unit Tests")
class ActivityServiceImplTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ActivityServiceImpl activityService;

    private User testUser;
    private User anotherUser;
    private Activity testActivity;
    private ActivityRequest activityRequest;

    @BeforeEach
    void setUp() {
        testUser = TestDataFactory.createUser(1L, "john@example.com", "John", "Doe");
        anotherUser = TestDataFactory.createUser(2L, "jane@example.com", "Jane", "Smith");
        testActivity = TestDataFactory.createActivity(1L, testUser);
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
    @DisplayName("getRecentActivitiesByUserId Tests")
    class GetRecentActivitiesTests {

        @Test
        @DisplayName("Should return limited recent activities")
        void shouldReturnLimitedRecentActivities() {
            Activity activity2 = TestDataFactory.createActivity(2L, testUser);
            when(activityRepository.findByUserId(eq(1L), any(Pageable.class)))
                    .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(testActivity, activity2)));

            List<Activity> result = activityService.getRecentActivitiesByUserId(1L, 2);

            assertThat(result).hasSize(2);
            verify(activityRepository).findByUserId(eq(1L), any(Pageable.class));
        }

        @Test
        @DisplayName("Should return empty list when no activities")
        void shouldReturnEmptyListWhenNoActivities() {
            when(activityRepository.findByUserId(eq(1L), any(Pageable.class)))
                    .thenReturn(new org.springframework.data.domain.PageImpl<>(Collections.emptyList()));

            List<Activity> result = activityService.getRecentActivitiesByUserId(1L, 5);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("createActivity Tests")
    class CreateActivityTests {

        @Test
        @DisplayName("Should create activity successfully")
        void shouldCreateActivitySuccessfully() {
            when(userService.getUserById(1L)).thenReturn(testUser);
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
            assertThat(result.getUser()).isEqualTo(testUser);
            verify(userService).getUserById(1L);
            verify(activityRepository).save(any(Activity.class));
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userService.getUserById(999L))
                    .thenThrow(new ApiRequestException("User not found", HttpStatus.NOT_FOUND));

            assertThatThrownBy(() -> activityService.createActivity(999L, activityRequest))
                    .isInstanceOf(ApiRequestException.class);
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
        @DisplayName("Should delete activity successfully when owner")
        void shouldDeleteActivitySuccessfullyWhenOwner() {
            when(activityRepository.findById(1L)).thenReturn(Optional.of(testActivity));

            activityService.deleteActivity(1L, 1L);

            verify(activityRepository).delete(testActivity);
        }

        @Test
        @DisplayName("Should throw exception when not owner")
        void shouldThrowExceptionWhenNotOwner() {
            when(activityRepository.findById(1L)).thenReturn(Optional.of(testActivity));

            assertThatThrownBy(() -> activityService.deleteActivity(1L, 2L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(ACTIVITY_ACCESS_DENIED);

            verify(activityRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("likeActivity Tests")
    class LikeActivityTests {

        @Test
        @DisplayName("Should like activity successfully")
        void shouldLikeActivitySuccessfully() {
            when(activityRepository.findById(1L)).thenReturn(Optional.of(testActivity));
            when(userService.getUserById(2L)).thenReturn(anotherUser);
            when(activityRepository.save(any(Activity.class))).thenReturn(testActivity);

            activityService.likeActivity(1L, 2L);

            assertThat(testActivity.getLikedBy()).contains(anotherUser);
            verify(activityRepository).save(testActivity);
        }

        @Test
        @DisplayName("Should throw exception when already liked")
        void shouldThrowExceptionWhenAlreadyLiked() {
            testActivity.getLikedBy().add(anotherUser);
            when(activityRepository.findById(1L)).thenReturn(Optional.of(testActivity));
            when(userService.getUserById(2L)).thenReturn(anotherUser);

            assertThatThrownBy(() -> activityService.likeActivity(1L, 2L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("Activity already liked");
        }
    }

    @Nested
    @DisplayName("unlikeActivity Tests")
    class UnlikeActivityTests {

        @Test
        @DisplayName("Should unlike activity successfully")
        void shouldUnlikeActivitySuccessfully() {
            testActivity.getLikedBy().add(anotherUser);
            when(activityRepository.findById(1L)).thenReturn(Optional.of(testActivity));
            when(userService.getUserById(2L)).thenReturn(anotherUser);
            when(activityRepository.save(any(Activity.class))).thenReturn(testActivity);

            activityService.unlikeActivity(1L, 2L);

            assertThat(testActivity.getLikedBy()).doesNotContain(anotherUser);
            verify(activityRepository).save(testActivity);
        }

        @Test
        @DisplayName("Should throw exception when not liked")
        void shouldThrowExceptionWhenNotLiked() {
            when(activityRepository.findById(1L)).thenReturn(Optional.of(testActivity));
            when(userService.getUserById(2L)).thenReturn(anotherUser);

            assertThatThrownBy(() -> activityService.unlikeActivity(1L, 2L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("Activity not liked");
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
            List<Object[]> dailyResults = new java.util.ArrayList<>();
            dailyResults.add(new Object[]{1, 50.0, 18000, 5});
            when(activityRepository.getDailyStatsByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(dailyResults);

            WeeklyStatsResponse result = activityService.getWeeklyStats(1L);

            assertThat(result.getTotalDistance()).isEqualTo(50.0);
            assertThat(result.getDailyStats()).hasSize(7);
        }
    }

    @Nested
    @DisplayName("getPublicActivities Tests")
    class GetPublicActivitiesTests {

        @Test
        @DisplayName("Should return public activities")
        void shouldReturnPublicActivities() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Activity> page = new PageImpl<>(List.of(testActivity));
            when(activityRepository.findAll(pageable)).thenReturn(page);

            Page<Activity> result = activityService.getPublicActivities(pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(activityRepository).findAll(pageable);
        }
    }

    @Nested
    @DisplayName("countActivitiesByUserId Tests")
    class CountActivitiesTests {

        @Test
        @DisplayName("Should return activity count")
        void shouldReturnActivityCount() {
            when(activityRepository.countByUserId(1L)).thenReturn(15);

            int result = activityService.countActivitiesByUserId(1L);

            assertThat(result).isEqualTo(15);
        }
    }

    @Nested
    @DisplayName("isLikedByUser Tests")
    class IsLikedByUserTests {

        @Test
        @DisplayName("Should return true when liked")
        void shouldReturnTrueWhenLiked() {
            when(activityRepository.isLikedByUser(1L, 2L)).thenReturn(true);

            boolean result = activityService.isLikedByUser(1L, 2L);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when not liked")
        void shouldReturnFalseWhenNotLiked() {
            when(activityRepository.isLikedByUser(1L, 2L)).thenReturn(false);

            boolean result = activityService.isLikedByUser(1L, 2L);

            assertThat(result).isFalse();
        }
    }
}
