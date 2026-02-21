package com.strava.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.backend.domain.Activity;
import com.strava.backend.domain.User;
import com.strava.backend.dto.activity.ActivityRequest;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.enums.AuthProvider;
import com.strava.backend.enums.Role;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.repository.UserRepository;
import com.strava.backend.security.UserPrincipal;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Activity API Integration Tests")
class ActivityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityRepository activityRepository;

    private User testUser;
    private Activity testActivity;
    private Authentication testAuth;

    @BeforeEach
    void setUp() {
        // Clean up
        activityRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = User.builder()
                .email("integration@test.com")
                .firstName("Integration")
                .lastName("Test")
                .password("password123")
                .authProvider(AuthProvider.LOCAL)
                .role(Role.USER)
                .active(true)
                .build();
        testUser = userRepository.save(testUser);
        UserPrincipal userPrincipal = UserPrincipal.create(testUser);
        testAuth = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());

        // Create test activity
        testActivity = Activity.builder()
                .title("Integration Test Run")
                .description("Activity for integration testing")
                .activityType(ActivityType.RUN)
                .distance(10.0)
                .duration(3600)
                .elevationGain(100)
                .calories(500)
                .startTime(LocalDateTime.now().minusHours(2))
                .isPrivate(false)
                .user(testUser)
                .build();
        testActivity = activityRepository.save(testActivity);
    }

    @Nested
    @DisplayName("GET /api/v1/activities/{id}")
    class GetActivityTests {

        @Test
        @DisplayName("Should return activity by ID")
        void shouldReturnActivityById() throws Exception {
            mockMvc.perform(get("/api/v1/activities/{id}", testActivity.getId())
                            .with(authentication(testAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testActivity.getId()))
                    .andExpect(jsonPath("$.title").value("Integration Test Run"))
                    .andExpect(jsonPath("$.activityType").value("RUN"))
                    .andExpect(jsonPath("$.distance").value(10.0))
                    .andExpect(jsonPath("$.duration").value(3600));
        }

        @Test
        @DisplayName("Should return 404 for non-existent activity")
        void shouldReturn404ForNonExistentActivity() throws Exception {
            mockMvc.perform(get("/api/v1/activities/{id}", 99999L)
                            .with(authentication(testAuth)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/activities")
    class CreateActivityTests {

        @Test
        @DisplayName("Should create new activity")
        void shouldCreateNewActivity() throws Exception {
            ActivityRequest request = new ActivityRequest();
            request.setTitle("New Integration Activity");
            request.setDescription("Created in integration test");
            request.setActivityType(ActivityType.RIDE);
            request.setDistance(25.0);
            request.setDuration(5400);
            request.setElevationGain(300);
            request.setCalories(800);
            request.setStartTime(LocalDateTime.now());
            request.setPrivate(false);

            mockMvc.perform(post("/api/v1/activities")
                            .with(authentication(testAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("New Integration Activity"))
                    .andExpect(jsonPath("$.activityType").value("RIDE"))
                    .andExpect(jsonPath("$.distance").value(25.0));

            // Verify in database
            assertThat(activityRepository.count()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return 400 for invalid request")
        void shouldReturn400ForInvalidRequest() throws Exception {
            ActivityRequest request = new ActivityRequest();
            request.setTitle(""); // Invalid - empty title
            request.setActivityType(ActivityType.RUN);
            request.setDistance(-10.0); // Invalid - negative distance
            request.setDuration(3600);
            request.setStartTime(LocalDateTime.now());

            mockMvc.perform(post("/api/v1/activities")
                            .with(authentication(testAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/activities/{id}")
    class UpdateActivityTests {

        @Test
        @DisplayName("Should update existing activity")
        void shouldUpdateExistingActivity() throws Exception {
            ActivityRequest request = new ActivityRequest();
            request.setTitle("Updated Activity Title");
            request.setDescription("Updated description");
            request.setActivityType(ActivityType.RUN);
            request.setDistance(15.0);
            request.setDuration(4500);
            request.setElevationGain(200);
            request.setCalories(700);
            request.setStartTime(LocalDateTime.now());
            request.setPrivate(false);

            mockMvc.perform(put("/api/v1/activities/{id}", testActivity.getId())
                            .with(authentication(testAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated Activity Title"))
                    .andExpect(jsonPath("$.distance").value(15.0));
        }

        @Test
        @DisplayName("Should return 403 when updating another user's activity")
        void shouldReturn403WhenUpdatingAnotherUsersActivity() throws Exception {
            User anotherUser = User.builder()
                    .email("another@test.com")
                    .firstName("Another")
                    .lastName("User")
                    .password("password123")
                    .authProvider(AuthProvider.LOCAL)
                    .role(Role.USER)
                    .active(true)
                    .build();
            anotherUser = userRepository.save(anotherUser);
            UserPrincipal anotherPrincipal = UserPrincipal.create(anotherUser);
            Authentication anotherAuth = new UsernamePasswordAuthenticationToken(anotherPrincipal, null, anotherPrincipal.getAuthorities());

            ActivityRequest request = new ActivityRequest();
            request.setTitle("Attempted Update");
            request.setActivityType(ActivityType.RUN);
            request.setDistance(10.0);
            request.setDuration(3600);
            request.setStartTime(LocalDateTime.now());

            mockMvc.perform(put("/api/v1/activities/{id}", testActivity.getId())
                            .with(authentication(anotherAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/activities/{id}")
    class DeleteActivityTests {

        @Test
        @DisplayName("Should delete activity")
        void shouldDeleteActivity() throws Exception {
            mockMvc.perform(delete("/api/v1/activities/{id}", testActivity.getId())
                            .with(authentication(testAuth)))
                    .andExpect(status().isNoContent());

            assertThat(activityRepository.findById(testActivity.getId())).isEmpty();
        }

        @Test
        @DisplayName("Should return 403 when deleting another user's activity")
        void shouldReturn403WhenDeletingAnotherUsersActivity() throws Exception {
            User anotherUser = User.builder()
                    .email("another2@test.com")
                    .firstName("Another")
                    .lastName("User")
                    .password("password123")
                    .authProvider(AuthProvider.LOCAL)
                    .role(Role.USER)
                    .active(true)
                    .build();
            anotherUser = userRepository.save(anotherUser);
            UserPrincipal anotherPrincipal = UserPrincipal.create(anotherUser);
            Authentication anotherAuth = new UsernamePasswordAuthenticationToken(anotherPrincipal, null, anotherPrincipal.getAuthorities());

            mockMvc.perform(delete("/api/v1/activities/{id}", testActivity.getId())
                            .with(authentication(anotherAuth)))
                    .andExpect(status().isForbidden());

            // Activity should still exist
            assertThat(activityRepository.findById(testActivity.getId())).isPresent();
        }
    }

    @Nested
    @DisplayName("Like/Unlike Activity Tests")
    class LikeActivityTests {

        @Test
        @DisplayName("Should like and unlike activity")
        void shouldLikeAndUnlikeActivity() throws Exception {
            User liker = User.builder()
                    .email("liker@test.com")
                    .firstName("Liker")
                    .lastName("User")
                    .password("password123")
                    .authProvider(AuthProvider.LOCAL)
                    .role(Role.USER)
                    .active(true)
                    .build();
            liker = userRepository.save(liker);
            UserPrincipal likerPrincipal = UserPrincipal.create(liker);
            Authentication likerAuth = new UsernamePasswordAuthenticationToken(likerPrincipal, null, likerPrincipal.getAuthorities());

            // Like activity
            mockMvc.perform(post("/api/v1/activities/{id}/like", testActivity.getId())
                            .with(authentication(likerAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Activity liked"));

            // Verify like
            assertThat(activityRepository.isLikedByUser(testActivity.getId(), liker.getId())).isTrue();

            // Unlike activity
            mockMvc.perform(delete("/api/v1/activities/{id}/like", testActivity.getId())
                            .with(authentication(likerAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Activity unliked"));

            // Verify unlike
            assertThat(activityRepository.isLikedByUser(testActivity.getId(), liker.getId())).isFalse();
        }

        @Test
        @DisplayName("Should return 400 when liking already liked activity")
        void shouldReturn400WhenLikingAlreadyLikedActivity() throws Exception {
            User liker = User.builder()
                    .email("liker2@test.com")
                    .firstName("Liker")
                    .lastName("User")
                    .password("password123")
                    .authProvider(AuthProvider.LOCAL)
                    .role(Role.USER)
                    .active(true)
                    .build();
            liker = userRepository.save(liker);
            UserPrincipal likerPrincipal = UserPrincipal.create(liker);
            Authentication likerAuth = new UsernamePasswordAuthenticationToken(likerPrincipal, null, likerPrincipal.getAuthorities());

            // First like
            mockMvc.perform(post("/api/v1/activities/{id}/like", testActivity.getId())
                            .with(authentication(likerAuth)))
                    .andExpect(status().isOk());

            // Second like should fail
            mockMvc.perform(post("/api/v1/activities/{id}/like", testActivity.getId())
                            .with(authentication(likerAuth)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Statistics Endpoints Tests")
    class StatisticsTests {

        @Test
        @DisplayName("Should return monthly stats")
        void shouldReturnMonthlyStats() throws Exception {
            // Create more activities for stats
            Activity activity2 = Activity.builder()
                    .title("Second Run")
                    .activityType(ActivityType.RUN)
                    .distance(5.0)
                    .duration(1800)
                    .elevationGain(50)
                    .calories(250)
                    .startTime(LocalDateTime.now().minusDays(5))
                    .isPrivate(false)
                    .user(testUser)
                    .build();
            activityRepository.save(activity2);

            mockMvc.perform(get("/api/v1/activities/stats/monthly")
                            .with(authentication(testAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalDistance").exists())
                    .andExpect(jsonPath("$.totalDuration").exists())
                    .andExpect(jsonPath("$.totalActivities").exists());
        }

        @Test
        @DisplayName("Should return weekly stats")
        void shouldReturnWeeklyStats() throws Exception {
            mockMvc.perform(get("/api/v1/activities/stats/weekly")
                            .with(authentication(testAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalDistance").exists())
                    .andExpect(jsonPath("$.dailyStats").isArray())
                    .andExpect(jsonPath("$.dailyStats", hasSize(7)));
        }
    }

    @Nested
    @DisplayName("Feed and List Endpoints Tests")
    class FeedAndListTests {

        @Test
        @DisplayName("Should return feed activities")
        void shouldReturnFeedActivities() throws Exception {
            mockMvc.perform(get("/api/v1/activities/feed")
                            .with(authentication(testAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should return user's activities")
        void shouldReturnUsersActivities() throws Exception {
            mockMvc.perform(get("/api/v1/activities/my")
                            .with(authentication(testAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].title").value("Integration Test Run"));
        }

        @Test
        @DisplayName("Should return public activities")
        void shouldReturnPublicActivities() throws Exception {
            mockMvc.perform(get("/api/v1/activities/public")
                            .with(authentication(testAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }
}
