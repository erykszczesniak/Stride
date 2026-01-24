package com.strava.backend.service.impl;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.Event;
import com.strava.backend.domain.Goal;
import com.strava.backend.domain.User;
import com.strava.backend.dto.DashboardResponse;
import com.strava.backend.dto.activity.ActivityResponse;
import com.strava.backend.dto.activity.ActivityStatsResponse;
import com.strava.backend.dto.activity.WeeklyStatsResponse;
import com.strava.backend.dto.event.EventResponse;
import com.strava.backend.dto.goal.GoalResponse;
import com.strava.backend.dto.user.UserResponse;
import com.strava.backend.enums.GoalPeriod;
import com.strava.backend.mapper.CommonMapper;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.repository.UserRepository;
import com.strava.backend.service.ActivityService;
import com.strava.backend.service.EventService;
import com.strava.backend.service.GoalService;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardServiceImpl Unit Tests")
class DashboardServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private ActivityService activityService;

    @Mock
    private GoalService goalService;

    @Mock
    private EventService eventService;

    @Mock
    private CommonMapper commonMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActivityRepository activityRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private User testUser;
    private Activity testActivity;
    private Goal testGoal;
    private Event testEvent;
    private UserResponse userResponse;
    private ActivityStatsResponse statsResponse;
    private WeeklyStatsResponse weeklyStatsResponse;
    private ActivityResponse activityResponse;
    private GoalResponse goalResponse;
    private EventResponse eventResponse;

    @BeforeEach
    void setUp() {
        testUser = TestDataFactory.createUser(1L, "john@example.com", "John", "Doe");
        testActivity = TestDataFactory.createActivity(1L, testUser);
        testGoal = TestDataFactory.createGoal(1L, testUser);
        testEvent = TestDataFactory.createEvent(1L, testUser);

        userResponse = new UserResponse();
        userResponse.setId(1L);
        userResponse.setEmail("john@example.com");
        userResponse.setFirstName("John");
        userResponse.setLastName("Doe");

        statsResponse = ActivityStatsResponse.builder()
                .totalDistance(100.0)
                .totalDuration(36000L)
                .totalCalories(5000)
                .totalElevationGain(1500)
                .totalActivities(10)
                .averageDistance(10.0)
                .build();

        weeklyStatsResponse = WeeklyStatsResponse.builder()
                .totalDistance(50.0)
                .trendPercentage("+10%")
                .dailyStats(Collections.emptyList())
                .build();

        activityResponse = new ActivityResponse();
        activityResponse.setId(1L);
        activityResponse.setTitle("Morning Run");

        goalResponse = new GoalResponse();
        goalResponse.setId(1L);
        goalResponse.setTargetValue(50.0);
        goalResponse.setCurrentValue(25.0);

        eventResponse = new EventResponse();
        eventResponse.setId(1L);
        eventResponse.setName("Marathon 2024");
    }

    @Nested
    @DisplayName("getDashboardData Tests")
    class GetDashboardDataTests {

        @Test
        @DisplayName("Should return complete dashboard data")
        void shouldReturnCompleteDashboardData() {
            // Given
            given(userService.getUserById(1L)).willReturn(testUser);
            given(commonMapper.convertToResponse(testUser, UserResponse.class)).willReturn(userResponse);
            given(userRepository.countFollowers(1L)).willReturn(50);
            given(userRepository.countFollowing(1L)).willReturn(30);
            given(activityRepository.countByUserId(1L)).willReturn(100);
            given(activityService.getMonthlyStats(1L)).willReturn(statsResponse);
            given(activityService.getWeeklyStats(1L)).willReturn(weeklyStatsResponse);
            given(activityService.getRecentActivitiesByUserId(1L, 5)).willReturn(List.of(testActivity));
            given(commonMapper.convertToResponse(testActivity, ActivityResponse.class)).willReturn(activityResponse);
            given(activityService.isLikedByUser(1L, 1L)).willReturn(false);
            given(goalService.getActiveGoalByPeriod(1L, GoalPeriod.WEEKLY)).willReturn(Optional.of(testGoal));
            given(commonMapper.convertToResponse(testGoal, GoalResponse.class)).willReturn(goalResponse);
            given(eventService.getUpcomingEventsByUserId(1L, 3)).willReturn(List.of(testEvent));
            given(commonMapper.convertToResponse(testEvent, EventResponse.class)).willReturn(eventResponse);
            given(eventService.isUserRegistered(1L, 1L)).willReturn(true);

            // When
            DashboardResponse result = dashboardService.getDashboardData(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUser()).isNotNull();
            assertThat(result.getUser().getFollowersCount()).isEqualTo(50);
            assertThat(result.getUser().getFollowingCount()).isEqualTo(30);
            assertThat(result.getUser().getActivitiesCount()).isEqualTo(100);
            assertThat(result.getStats()).isEqualTo(statsResponse);
            assertThat(result.getWeeklyStats()).isEqualTo(weeklyStatsResponse);
            assertThat(result.getRecentActivities()).hasSize(1);
            assertThat(result.getWeeklyGoal()).isNotNull();
            assertThat(result.getUpcomingEvents()).hasSize(1);

            then(userService).should().getUserById(1L);
            then(activityService).should().getMonthlyStats(1L);
            then(activityService).should().getWeeklyStats(1L);
        }

        @Test
        @DisplayName("Should return dashboard data without weekly goal")
        void shouldReturnDashboardDataWithoutWeeklyGoal() {
            // Given
            given(userService.getUserById(1L)).willReturn(testUser);
            given(commonMapper.convertToResponse(testUser, UserResponse.class)).willReturn(userResponse);
            given(userRepository.countFollowers(1L)).willReturn(0);
            given(userRepository.countFollowing(1L)).willReturn(0);
            given(activityRepository.countByUserId(1L)).willReturn(0);
            given(activityService.getMonthlyStats(1L)).willReturn(statsResponse);
            given(activityService.getWeeklyStats(1L)).willReturn(weeklyStatsResponse);
            given(activityService.getRecentActivitiesByUserId(1L, 5)).willReturn(Collections.emptyList());
            given(goalService.getActiveGoalByPeriod(1L, GoalPeriod.WEEKLY)).willReturn(Optional.empty());
            given(eventService.getUpcomingEventsByUserId(1L, 3)).willReturn(Collections.emptyList());

            // When
            DashboardResponse result = dashboardService.getDashboardData(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getWeeklyGoal()).isNull();
            assertThat(result.getRecentActivities()).isEmpty();
            assertThat(result.getUpcomingEvents()).isEmpty();
        }

        @Test
        @DisplayName("Should return dashboard data with multiple activities")
        void shouldReturnDashboardDataWithMultipleActivities() {
            // Given
            Activity activity2 = TestDataFactory.createActivity(2L, testUser);
            ActivityResponse activityResponse2 = new ActivityResponse();
            activityResponse2.setId(2L);
            activityResponse2.setTitle("Evening Run");

            given(userService.getUserById(1L)).willReturn(testUser);
            given(commonMapper.convertToResponse(testUser, UserResponse.class)).willReturn(userResponse);
            given(userRepository.countFollowers(1L)).willReturn(10);
            given(userRepository.countFollowing(1L)).willReturn(5);
            given(activityRepository.countByUserId(1L)).willReturn(20);
            given(activityService.getMonthlyStats(1L)).willReturn(statsResponse);
            given(activityService.getWeeklyStats(1L)).willReturn(weeklyStatsResponse);
            given(activityService.getRecentActivitiesByUserId(1L, 5)).willReturn(List.of(testActivity, activity2));
            given(commonMapper.convertToResponse(testActivity, ActivityResponse.class)).willReturn(activityResponse);
            given(commonMapper.convertToResponse(activity2, ActivityResponse.class)).willReturn(activityResponse2);
            given(activityService.isLikedByUser(anyLong(), eq(1L))).willReturn(false);
            given(goalService.getActiveGoalByPeriod(1L, GoalPeriod.WEEKLY)).willReturn(Optional.empty());
            given(eventService.getUpcomingEventsByUserId(1L, 3)).willReturn(Collections.emptyList());

            // When
            DashboardResponse result = dashboardService.getDashboardData(1L);

            // Then
            assertThat(result.getRecentActivities()).hasSize(2);
        }
    }
}
