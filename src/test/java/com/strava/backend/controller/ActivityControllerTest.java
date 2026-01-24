package com.strava.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.strava.backend.dto.HeaderResponse;
import com.strava.backend.dto.activity.*;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.mapper.ActivityMapper;
import com.strava.backend.mapper.CommonMapper;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.security.CustomUserDetailsService;
import com.strava.backend.security.jwt.JwtProvider;
import com.strava.backend.service.ActivityService;
import com.strava.backend.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ActivityController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ActivityController Unit Tests")
class ActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivityMapper activityMapper;

    @MockitoBean
    private ActivityService activityService;

    @MockitoBean
    private ActivityRepository activityRepository;

    @MockitoBean
    private CommonMapper commonMapper;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private ObjectMapper objectMapper;
    private ActivityRequest activityRequest;
    private ActivityResponse activityResponse;
    private FullActivityResponse fullActivityResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        activityRequest = TestDataFactory.createActivityRequest();

        activityResponse = new ActivityResponse();
        activityResponse.setId(1L);
        activityResponse.setTitle("Morning Run");
        activityResponse.setActivityType(ActivityType.RUN);
        activityResponse.setDistance(10.5);
        activityResponse.setDuration(3600);
        activityResponse.setLikesCount(5);
        activityResponse.setCommentsCount(2);
        activityResponse.setCreatedAt(LocalDateTime.now());

        fullActivityResponse = new FullActivityResponse();
        fullActivityResponse.setId(1L);
        fullActivityResponse.setTitle("Morning Run");
        fullActivityResponse.setActivityType(ActivityType.RUN);
        fullActivityResponse.setDistance(10.5);
        fullActivityResponse.setDuration(3600);
        fullActivityResponse.setLikesCount(5);
        fullActivityResponse.setCommentsCount(2);
        fullActivityResponse.setComments(Collections.emptyList());
    }

    @Nested
    @DisplayName("GET /api/v1/activities/{activityId}")
    class GetActivityByIdTests {

        @Test
        @DisplayName("Should return activity when found")
        void shouldReturnActivityWhenFound() throws Exception {
            when(activityMapper.getActivityById(1L, 1L)).thenReturn(fullActivityResponse);

            mockMvc.perform(get("/api/v1/activities/1")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.title").value("Morning Run"))
                    .andExpect(jsonPath("$.activityType").value("RUN"))
                    .andExpect(jsonPath("$.distance").value(10.5));

            verify(activityMapper).getActivityById(1L, 1L);
        }

        @Test
        @DisplayName("Should use default user ID when not provided")
        void shouldUseDefaultUserIdWhenNotProvided() throws Exception {
            when(activityMapper.getActivityById(1L, 1L)).thenReturn(fullActivityResponse);

            mockMvc.perform(get("/api/v1/activities/1"))
                    .andExpect(status().isOk());

            verify(activityMapper).getActivityById(1L, 1L);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/activities/feed")
    class GetFeedActivitiesTests {

        @Test
        @DisplayName("Should return feed activities")
        void shouldReturnFeedActivities() throws Exception {
            HeaderResponse<ActivityResponse> response = HeaderResponse.of(
                    List.of(activityResponse), 1, 1L);
            when(activityMapper.getFeedActivities(eq(1L), any(Pageable.class))).thenReturn(response);

            mockMvc.perform(get("/api/v1/activities/feed")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].title").value("Morning Run"));
        }

        @Test
        @DisplayName("Should return empty list when no activities")
        void shouldReturnEmptyListWhenNoActivities() throws Exception {
            HeaderResponse<ActivityResponse> response = HeaderResponse.of(
                    Collections.emptyList(), 0, 0L);
            when(activityMapper.getFeedActivities(eq(1L), any(Pageable.class))).thenReturn(response);

            mockMvc.perform(get("/api/v1/activities/feed"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/activities")
    class CreateActivityTests {

        @Test
        @DisplayName("Should create activity successfully")
        void shouldCreateActivitySuccessfully() throws Exception {
            when(activityMapper.createActivity(eq(1L), any(ActivityRequest.class), any()))
                    .thenReturn(activityResponse);

            mockMvc.perform(post("/api/v1/activities")
                            .param("userId", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(activityRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.title").value("Morning Run"));
        }

        @Test
        @DisplayName("Should return bad request when title is empty")
        void shouldReturnBadRequestWhenTitleIsEmpty() throws Exception {
            activityRequest.setTitle("");

            // Controller passes BindingResult to mapper, which throws exception on validation errors
            when(activityMapper.createActivity(anyLong(), any(ActivityRequest.class), any()))
                    .thenThrow(new ApiRequestException("Title must not be blank", HttpStatus.BAD_REQUEST));

            mockMvc.perform(post("/api/v1/activities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(activityRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return bad request when distance is null")
        void shouldReturnBadRequestWhenDistanceIsNull() throws Exception {
            activityRequest.setDistance(null);

            // Controller passes BindingResult to mapper, which throws exception on validation errors
            when(activityMapper.createActivity(anyLong(), any(ActivityRequest.class), any()))
                    .thenThrow(new ApiRequestException("Distance is required", HttpStatus.BAD_REQUEST));

            mockMvc.perform(post("/api/v1/activities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(activityRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/activities/{activityId}")
    class UpdateActivityTests {

        @Test
        @DisplayName("Should update activity successfully")
        void shouldUpdateActivitySuccessfully() throws Exception {
            activityResponse.setTitle("Updated Run");
            when(activityMapper.updateActivity(eq(1L), eq(1L), any(ActivityRequest.class), any()))
                    .thenReturn(activityResponse);

            mockMvc.perform(put("/api/v1/activities/1")
                            .param("userId", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(activityRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated Run"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/activities/{activityId}")
    class DeleteActivityTests {

        @Test
        @DisplayName("Should delete activity successfully")
        void shouldDeleteActivitySuccessfully() throws Exception {
            doNothing().when(activityMapper).deleteActivity(1L, 1L);

            mockMvc.perform(delete("/api/v1/activities/1")
                            .param("userId", "1"))
                    .andExpect(status().isNoContent());

            verify(activityMapper).deleteActivity(1L, 1L);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/activities/{activityId}/like")
    class LikeActivityTests {

        @Test
        @DisplayName("Should like activity successfully")
        void shouldLikeActivitySuccessfully() throws Exception {
            doNothing().when(activityService).likeActivity(1L, 1L);

            mockMvc.perform(post("/api/v1/activities/1/like")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Activity liked"));

            verify(activityService).likeActivity(1L, 1L);
        }

        @Test
        @DisplayName("Should return error when already liked")
        void shouldReturnErrorWhenAlreadyLiked() throws Exception {
            doThrow(new ApiRequestException("Activity already liked", HttpStatus.BAD_REQUEST))
                    .when(activityService).likeActivity(1L, 1L);

            mockMvc.perform(post("/api/v1/activities/1/like")
                            .param("userId", "1"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/activities/{activityId}/like")
    class UnlikeActivityTests {

        @Test
        @DisplayName("Should unlike activity successfully")
        void shouldUnlikeActivitySuccessfully() throws Exception {
            doNothing().when(activityService).unlikeActivity(1L, 1L);

            mockMvc.perform(delete("/api/v1/activities/1/like")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Activity unliked"));

            verify(activityService).unlikeActivity(1L, 1L);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/activities/stats/monthly")
    class GetMonthlyStatsTests {

        @Test
        @DisplayName("Should return monthly stats")
        void shouldReturnMonthlyStats() throws Exception {
            ActivityStatsResponse statsResponse = ActivityStatsResponse.builder()
                    .totalDistance(100.0)
                    .totalDuration(36000L)
                    .totalCalories(5000)
                    .totalActivities(10)
                    .totalElevationGain(1500)
                    .averageDistance(10.0)
                    .averageDuration(3600.0)
                    .trendPercentage("+15.5%")
                    .build();

            when(activityMapper.getMonthlyStats(1L)).thenReturn(statsResponse);

            mockMvc.perform(get("/api/v1/activities/stats/monthly")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalDistance").value(100.0))
                    .andExpect(jsonPath("$.totalActivities").value(10))
                    .andExpect(jsonPath("$.trendPercentage").value("+15.5%"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/activities/stats/weekly")
    class GetWeeklyStatsTests {

        @Test
        @DisplayName("Should return weekly stats")
        void shouldReturnWeeklyStats() throws Exception {
            WeeklyStatsResponse weeklyStats = WeeklyStatsResponse.builder()
                    .totalDistance(50.0)
                    .trendPercentage("+10%")
                    .dailyStats(List.of(
                            WeeklyStatsResponse.DailyStats.builder()
                                    .dayName("Mon")
                                    .distance(10.0)
                                    .duration(3600)
                                    .activities(1)
                                    .build()
                    ))
                    .build();

            when(activityMapper.getWeeklyStats(1L)).thenReturn(weeklyStats);

            mockMvc.perform(get("/api/v1/activities/stats/weekly")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalDistance").value(50.0))
                    .andExpect(jsonPath("$.trendPercentage").value("+10%"))
                    .andExpect(jsonPath("$.dailyStats[0].dayName").value("Mon"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/activities/user/{targetUserId}")
    class GetUserActivitiesTests {

        @Test
        @DisplayName("Should return user activities")
        void shouldReturnUserActivities() throws Exception {
            HeaderResponse<ActivityResponse> response = HeaderResponse.of(
                    List.of(activityResponse), 1, 1L);
            when(activityMapper.getActivitiesByUserId(eq(2L), eq(1L), any(Pageable.class)))
                    .thenReturn(response);

            mockMvc.perform(get("/api/v1/activities/user/2")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/activities/my")
    class GetMyActivitiesTests {

        @Test
        @DisplayName("Should return current user activities")
        void shouldReturnCurrentUserActivities() throws Exception {
            HeaderResponse<ActivityResponse> response = HeaderResponse.of(
                    List.of(activityResponse), 1, 1L);
            when(activityMapper.getActivitiesByUserId(eq(1L), eq(1L), any(Pageable.class)))
                    .thenReturn(response);

            mockMvc.perform(get("/api/v1/activities/my")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/activities/public")
    class GetPublicActivitiesTests {

        @Test
        @DisplayName("Should return public activities")
        void shouldReturnPublicActivities() throws Exception {
            Page<com.strava.backend.domain.Activity> page = new PageImpl<>(Collections.emptyList());
            when(activityService.getPublicActivities(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/activities/public"))
                    .andExpect(status().isOk());
        }
    }
}
