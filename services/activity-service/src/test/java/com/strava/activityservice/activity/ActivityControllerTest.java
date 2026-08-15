package com.strava.activityservice.activity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.strava.activityservice.common.HeaderResponse;
import com.strava.activityservice.config.SecurityConfig;
import com.strava.activityservice.domain.ActivityType;
import com.strava.activityservice.exception.ApiRequestException;
import com.strava.activityservice.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.strava.activityservice.constants.ErrorMessage.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ActivityController.class)
@Import(SecurityConfig.class)
@DisplayName("ActivityController Unit Tests")
class ActivityControllerTest {

    private static final String USER_HEADER = "X-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivityMapper activityMapper;

    @MockitoBean
    private ActivityService activityService;

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
    @DisplayName("Security")
    class Security {

        @Test
        @DisplayName("requests without gateway identity headers are rejected")
        void unauthenticatedRejected() throws Exception {
            mockMvc.perform(get("/api/v1/activities/1"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/activities/{activityId}")
    class GetActivityByIdTests {

        @Test
        @DisplayName("Should return activity when found")
        void shouldReturnActivityWhenFound() throws Exception {
            when(activityMapper.getActivityById(1L, 1L)).thenReturn(fullActivityResponse);

            mockMvc.perform(get("/api/v1/activities/1").header(USER_HEADER, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.title").value("Morning Run"))
                    .andExpect(jsonPath("$.activityType").value("RUN"))
                    .andExpect(jsonPath("$.distance").value(10.5));

            verify(activityMapper).getActivityById(1L, 1L);
        }

        @Test
        @DisplayName("Denied/missing activities surface a 404 ProblemDetail with a message property")
        void notFoundProblemDetail() throws Exception {
            when(activityMapper.getActivityById(1L, 1L))
                    .thenThrow(new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND));

            mockMvc.perform(get("/api/v1/activities/1").header(USER_HEADER, "1"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(ACTIVITY_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/activities")
    class CreateActivityTests {

        @Test
        @DisplayName("Should create activity successfully")
        void shouldCreateActivitySuccessfully() throws Exception {
            when(activityMapper.createActivity(eq(1L), any(ActivityRequest.class)))
                    .thenReturn(activityResponse);

            mockMvc.perform(post("/api/v1/activities")
                            .header(USER_HEADER, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(activityRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.title").value("Morning Run"));
        }

        @Test
        @DisplayName("Should return bad request with main's message when title is empty")
        void shouldReturnBadRequestWhenTitleIsEmpty() throws Exception {
            activityRequest.setTitle("");

            mockMvc.perform(post("/api/v1/activities")
                            .header(USER_HEADER, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(activityRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(EMPTY_ACTIVITY_TITLE))
                    .andExpect(jsonPath("$.fieldErrors.title").value(EMPTY_ACTIVITY_TITLE));

            verify(activityMapper, never()).createActivity(anyLong(), any());
        }

        @Test
        @DisplayName("Should return bad request when distance is null")
        void shouldReturnBadRequestWhenDistanceIsNull() throws Exception {
            activityRequest.setDistance(null);

            mockMvc.perform(post("/api/v1/activities")
                            .header(USER_HEADER, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(activityRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.distance").value(INVALID_DISTANCE));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/activities/{activityId}")
    class UpdateActivityTests {

        @Test
        @DisplayName("Should update activity successfully")
        void shouldUpdateActivitySuccessfully() throws Exception {
            activityResponse.setTitle("Updated Run");
            when(activityMapper.updateActivity(eq(1L), eq(1L), any(ActivityRequest.class)))
                    .thenReturn(activityResponse);

            mockMvc.perform(put("/api/v1/activities/1")
                            .header(USER_HEADER, "1")
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

            mockMvc.perform(delete("/api/v1/activities/1").header(USER_HEADER, "1"))
                    .andExpect(status().isNoContent());

            verify(activityMapper).deleteActivity(1L, 1L);
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

            mockMvc.perform(get("/api/v1/activities/stats/monthly").header(USER_HEADER, "1"))
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

            mockMvc.perform(get("/api/v1/activities/stats/weekly").header(USER_HEADER, "1"))
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
        @DisplayName("Should return user activities with pagination headers")
        void shouldReturnUserActivities() throws Exception {
            HeaderResponse<ActivityResponse> response = HeaderResponse.of(
                    List.of(activityResponse), 1, 1L);
            when(activityMapper.getActivitiesByUserId(eq(2L), eq(1L), any(Pageable.class)))
                    .thenReturn(response);

            mockMvc.perform(get("/api/v1/activities/user/2").header(USER_HEADER, "1"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("page-total-count", "1"))
                    .andExpect(header().string("page-total-elements", "1"))
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

            mockMvc.perform(get("/api/v1/activities/my").header(USER_HEADER, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1));
        }

        @Test
        @DisplayName("Should search user activities by search query")
        void shouldSearchUserActivitiesBySearchQuery() throws Exception {
            when(activityMapper.searchMyActivities(eq(1L), eq("Morning"), isNull(), any(Pageable.class)))
                    .thenReturn(List.of(activityResponse));

            mockMvc.perform(get("/api/v1/activities/my")
                            .header(USER_HEADER, "1")
                            .param("search", "Morning"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].title").value("Morning Run"));

            verify(activityMapper).searchMyActivities(eq(1L), eq("Morning"), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("Should filter user activities by activity type")
        void shouldFilterUserActivitiesByActivityType() throws Exception {
            when(activityMapper.getActivitiesByType(eq(1L), eq(ActivityType.RUN), any(Pageable.class)))
                    .thenReturn(List.of(activityResponse));

            mockMvc.perform(get("/api/v1/activities/my")
                            .header(USER_HEADER, "1")
                            .param("activityType", "RUN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].activityType").value("RUN"));

            verify(activityMapper).getActivitiesByType(eq(1L), eq(ActivityType.RUN), any(Pageable.class));
        }

        @Test
        @DisplayName("Should return empty list when search has no matches")
        void shouldReturnEmptyListWhenSearchHasNoMatches() throws Exception {
            when(activityMapper.searchMyActivities(eq(1L), eq("nonexistent"), isNull(), any(Pageable.class)))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/activities/my")
                            .header(USER_HEADER, "1")
                            .param("search", "nonexistent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/activities/my/stats")
    class GetMyTotalStatsTests {

        @Test
        @DisplayName("Should return total stats for user")
        void shouldReturnTotalStatsForUser() throws Exception {
            when(activityMapper.getUserTotalStats(1L)).thenReturn(
                    Map.of("totalActivities", 10, "totalDistanceKm", 150.5, "totalDurationSeconds", 36000L));

            mockMvc.perform(get("/api/v1/activities/my/stats").header(USER_HEADER, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalActivities").value(10))
                    .andExpect(jsonPath("$.totalDistanceKm").value(150.5))
                    .andExpect(jsonPath("$.totalDurationSeconds").value(36000));
        }
    }

    @Nested
    @DisplayName("Photos")
    class PhotoTests {

        @Test
        @DisplayName("POST /{id}/photos returns 201 with the photo URL list")
        void uploadPhotos() throws Exception {
            when(activityService.uploadPhotos(eq(1L), eq(1L), anyList()))
                    .thenReturn(List.of("/uploads/photos/a.jpg"));

            MockMultipartFile file = new MockMultipartFile(
                    "files", "a.jpg", "image/jpeg", new byte[]{1, 2, 3});

            mockMvc.perform(multipart("/api/v1/activities/1/photos")
                            .file(file)
                            .header(USER_HEADER, "1"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$[0]").value("/uploads/photos/a.jpg"));
        }

        @Test
        @DisplayName("DELETE /{id}/photos/{index} returns 204")
        void deletePhoto() throws Exception {
            mockMvc.perform(delete("/api/v1/activities/1/photos/0").header(USER_HEADER, "1"))
                    .andExpect(status().isNoContent());

            verify(activityService).deletePhoto(1L, 1L, 0);
        }
    }
}
