package com.strava.backend.controller;

import com.strava.backend.dto.DashboardResponse;
import com.strava.backend.dto.activity.ActivityStatsResponse;
import com.strava.backend.dto.activity.WeeklyStatsResponse;
import com.strava.backend.dto.user.UserResponse;
import com.strava.backend.security.CustomUserDetailsService;
import com.strava.backend.security.UserPrincipal;
import com.strava.backend.security.jwt.JwtProvider;
import com.strava.backend.service.DashboardService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("DashboardController Unit Tests")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private DashboardResponse dashboardResponse;

    @BeforeEach
    void setUp() {
        UserResponse userResponse = new UserResponse();
        userResponse.setId(1L);
        userResponse.setFirstName("John");
        userResponse.setLastName("Doe");
        userResponse.setEmail("john@example.com");

        ActivityStatsResponse stats = ActivityStatsResponse.builder()
                .totalDistance(150.0)
                .totalDuration(54000L)
                .totalCalories(7500)
                .totalActivities(15)
                .totalElevationGain(2000)
                .averageDistance(10.0)
                .averageDuration(3600.0)
                .trendPercentage("+12.5%")
                .build();

        WeeklyStatsResponse weeklyStats = WeeklyStatsResponse.builder()
                .totalDistance(50.0)
                .trendPercentage("+8%")
                .dailyStats(Collections.emptyList())
                .build();

        dashboardResponse = DashboardResponse.builder()
                .user(userResponse)
                .stats(stats)
                .weeklyStats(weeklyStats)
                .recentActivities(Collections.emptyList())
                .weeklyGoal(null)
                .upcomingEvents(Collections.emptyList())
                .build();

        UserPrincipal userPrincipal = UserPrincipal.builder()
                .id(1L)
                .email("test@example.com")
                .password("password")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("GET /api/v1/dashboard")
    class GetDashboardTests {

        @Test
        @DisplayName("Should return dashboard data")
        void shouldReturnDashboardData() throws Exception {
            given(dashboardService.getDashboardData(1L)).willReturn(dashboardResponse);

            mockMvc.perform(get("/api/v1/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.id").value(1))
                    .andExpect(jsonPath("$.user.firstName").value("John"))
                    .andExpect(jsonPath("$.stats.totalDistance").value(150.0))
                    .andExpect(jsonPath("$.stats.totalActivities").value(15))
                    .andExpect(jsonPath("$.weeklyStats.totalDistance").value(50.0));
        }

        @Test
        @DisplayName("Should use default user ID when not provided")
        void shouldUseDefaultUserIdWhenNotProvided() throws Exception {
            given(dashboardService.getDashboardData(1L)).willReturn(dashboardResponse);

            mockMvc.perform(get("/api/v1/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.id").value(1));
        }

        @Test
        @DisplayName("Should return dashboard with empty recent activities")
        void shouldReturnDashboardWithEmptyRecentActivities() throws Exception {
            given(dashboardService.getDashboardData(1L)).willReturn(dashboardResponse);

            mockMvc.perform(get("/api/v1/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.recentActivities").isEmpty());
        }

        @Test
        @DisplayName("Should return dashboard with null weekly goal")
        void shouldReturnDashboardWithNullWeeklyGoal() throws Exception {
            given(dashboardService.getDashboardData(1L)).willReturn(dashboardResponse);

            mockMvc.perform(get("/api/v1/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.weeklyGoal").doesNotExist());
        }
    }
}
