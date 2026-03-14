package com.strava.backend.graphql;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.User;
import com.strava.backend.dto.activity.ActivityStatsResponse;
import com.strava.backend.dto.activity.WeeklyStatsResponse;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.enums.AuthProvider;
import com.strava.backend.enums.Role;
import com.strava.backend.service.ActivityService;
import com.strava.backend.service.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;

import com.strava.backend.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@GraphQlTest(ActivityGraphQLController.class)
@ActiveProfiles("test")
@DisplayName("ActivityGraphQLController Tests")
class ActivityGraphQLControllerTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ActivityService activityService;

    @MockitoBean
    private CommentService commentService;

    private User testUser;
    private Activity testActivity;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .password("password")
                .authProvider(AuthProvider.LOCAL)
                .role(Role.USER)
                .active(true)
                .build();

        UserPrincipal principal = UserPrincipal.create(testUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        testActivity = Activity.builder()
                .id(1L)
                .title("Morning Run")
                .description("A nice morning run")
                .activityType(ActivityType.RUN)
                .distance(10.0)
                .duration(3600)
                .elevationGain(100)
                .calories(500)
                .averageSpeed(10.0)
                .startTime(LocalDateTime.now().minusHours(2))
                .isPrivate(false)
                .user(testUser)
                .likedBy(new HashSet<>())
                .comments(new HashSet<>())
                .build();
        testActivity.setCreatedAt(LocalDateTime.now());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Query: activity")
    class ActivityQueryTests {

        @Test
        @DisplayName("Should return activity by ID")
        void shouldReturnActivityById() {
            when(activityService.getActivityById(1L)).thenReturn(testActivity);
            when(activityService.isLikedByUser(1L, 1L)).thenReturn(false);
            when(commentService.getCommentsByActivityId(1L)).thenReturn(Collections.emptyList());

            graphQlTester.document("""
                query {
                    activity(id: 1) {
                        id
                        title
                        activityType
                        distance
                        duration
                    }
                }
                """)
                    .execute()
                    .path("activity.id").entity(Long.class).isEqualTo(1L)
                    .path("activity.title").entity(String.class).isEqualTo("Morning Run")
                    .path("activity.activityType").entity(String.class).isEqualTo("RUN")
                    .path("activity.distance").entity(Double.class).isEqualTo(10.0);
        }
    }

    @Nested
    @DisplayName("Query: activities")
    class ActivitiesQueryTests {

        @Test
        @DisplayName("Should return list of activities")
        void shouldReturnListOfActivities() {
            Page<Activity> page = new PageImpl<>(List.of(testActivity));
            when(activityService.getActivitiesByUserId(eq(1L), any(Pageable.class))).thenReturn(page);
            when(activityService.isLikedByUser(anyLong(), anyLong())).thenReturn(false);
            when(commentService.getCommentsByActivityId(anyLong())).thenReturn(Collections.emptyList());

            graphQlTester.document("""
                query {
                    activities(userId: 1, page: 0, size: 10) {
                        id
                        title
                        distance
                    }
                }
                """)
                    .execute()
                    .path("activities").entityList(Object.class).hasSize(1)
                    .path("activities[0].title").entity(String.class).isEqualTo("Morning Run");
        }

        @Test
        @DisplayName("Should return empty list when no activities")
        void shouldReturnEmptyListWhenNoActivities() {
            Page<Activity> emptyPage = new PageImpl<>(Collections.emptyList());
            when(activityService.getActivitiesByUserId(eq(1L), any(Pageable.class))).thenReturn(emptyPage);

            graphQlTester.document("""
                query {
                    activities(userId: 1) {
                        id
                        title
                    }
                }
                """)
                    .execute()
                    .path("activities").entityList(Object.class).hasSize(0);
        }
    }

    @Nested
    @DisplayName("Query: feedActivities")
    class FeedActivitiesQueryTests {

        @Test
        @DisplayName("Should return feed activities")
        void shouldReturnFeedActivities() {
            Page<Activity> page = new PageImpl<>(List.of(testActivity));
            when(activityService.getFeedActivities(eq(1L), any(Pageable.class))).thenReturn(page);
            when(activityService.isLikedByUser(anyLong(), anyLong())).thenReturn(false);
            when(commentService.getCommentsByActivityId(anyLong())).thenReturn(Collections.emptyList());

            graphQlTester.document("""
                query {
                    feedActivities(page: 0, size: 10) {
                        id
                        title
                    }
                }
                """)
                    .execute()
                    .path("feedActivities").entityList(Object.class).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Query: activityStats")
    class ActivityStatsQueryTests {

        @Test
        @DisplayName("Should return monthly activity stats")
        void shouldReturnMonthlyActivityStats() {
            ActivityStatsResponse stats = ActivityStatsResponse.builder()
                    .totalDistance(100.0)
                    .totalDuration(36000L)
                    .totalCalories(5000)
                    .totalActivities(10)
                    .totalElevationGain(1000)
                    .averageDistance(10.0)
                    .averageDuration(3600.0)
                    .trendPercentage("+15%")
                    .build();

            when(activityService.getMonthlyStats(1L)).thenReturn(stats);

            graphQlTester.document("""
                query {
                    activityStats(userId: 1) {
                        totalDistance
                        totalDuration
                        totalActivities
                        trendPercentage
                    }
                }
                """)
                    .execute()
                    .path("activityStats.totalDistance").entity(Double.class).isEqualTo(100.0)
                    .path("activityStats.totalActivities").entity(Integer.class).isEqualTo(10)
                    .path("activityStats.trendPercentage").entity(String.class).isEqualTo("+15%");
        }
    }

    @Nested
    @DisplayName("Query: weeklyStats")
    class WeeklyStatsQueryTests {

        @Test
        @DisplayName("Should return weekly stats with daily breakdown")
        void shouldReturnWeeklyStatsWithDailyBreakdown() {
            WeeklyStatsResponse.DailyStats monday = WeeklyStatsResponse.DailyStats.builder()
                    .dayName("Mon")
                    .distance(10.0)
                    .duration(3600)
                    .activities(1)
                    .build();

            WeeklyStatsResponse weeklyStats = WeeklyStatsResponse.builder()
                    .totalDistance(50.0)
                    .trendPercentage("+10%")
                    .dailyStats(List.of(monday))
                    .build();

            when(activityService.getWeeklyStats(1L)).thenReturn(weeklyStats);

            graphQlTester.document("""
                query {
                    weeklyStats(userId: 1) {
                        totalDistance
                        trendPercentage
                        dailyStats {
                            dayName
                            distance
                            activities
                        }
                    }
                }
                """)
                    .execute()
                    .path("weeklyStats.totalDistance").entity(Double.class).isEqualTo(50.0)
                    .path("weeklyStats.dailyStats[0].dayName").entity(String.class).isEqualTo("Mon");
        }
    }

    @Nested
    @DisplayName("Mutation: deleteActivity")
    class DeleteActivityMutationTests {

        @Test
        @DisplayName("Should delete activity and return true")
        void shouldDeleteActivityAndReturnTrue() {
            graphQlTester.document("""
                mutation {
                    deleteActivity(id: 1)
                }
                """)
                    .execute()
                    .path("deleteActivity").entity(Boolean.class).isEqualTo(true);
        }
    }

    @Nested
    @DisplayName("Mutation: likeActivity")
    class LikeActivityMutationTests {

        @Test
        @DisplayName("Should like activity and return updated activity")
        void shouldLikeActivityAndReturnUpdatedActivity() {
            when(activityService.getActivityById(1L)).thenReturn(testActivity);
            when(activityService.isLikedByUser(1L, 1L)).thenReturn(true);
            when(commentService.getCommentsByActivityId(1L)).thenReturn(Collections.emptyList());

            graphQlTester.document("""
                mutation {
                    likeActivity(id: 1) {
                        id
                        title
                        likedByCurrentUser
                    }
                }
                """)
                    .execute()
                    .path("likeActivity.id").entity(Long.class).isEqualTo(1L)
                    .path("likeActivity.likedByCurrentUser").entity(Boolean.class).isEqualTo(true);
        }
    }

    @Nested
    @DisplayName("Schema Mapping Tests")
    class SchemaMappingTests {

        @Test
        @DisplayName("Should resolve likesCount field")
        void shouldResolveLikesCountField() {
            testActivity.getLikedBy().add(testUser);
            when(activityService.getActivityById(1L)).thenReturn(testActivity);
            when(activityService.isLikedByUser(1L, 1L)).thenReturn(false);
            when(commentService.getCommentsByActivityId(1L)).thenReturn(Collections.emptyList());

            graphQlTester.document("""
                query {
                    activity(id: 1) {
                        id
                        likesCount
                    }
                }
                """)
                    .execute()
                    .path("activity.likesCount").entity(Integer.class).isEqualTo(1);
        }
    }
}
