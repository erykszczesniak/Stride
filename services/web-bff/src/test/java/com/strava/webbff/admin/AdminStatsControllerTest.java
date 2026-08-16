package com.strava.webbff.admin;

import com.strava.webbff.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminStatsController.class)
@Import(SecurityConfig.class)
@DisplayName("AdminStatsController Unit Tests")
class AdminStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminStatsService adminStatsService;

    @Test
    @DisplayName("Should return the stats with main's exact field names for an admin")
    void shouldReturnStatsForAdmin() throws Exception {
        given(adminStatsService.getAdminStats()).willReturn(AdminStatsResponse.builder()
                .totalUsers(42)
                .activeUsers(40)
                .totalActivities(1234)
                .totalClubs(3)
                .totalChallenges(5)
                .totalEvents(7)
                .newUsersThisWeek(2)
                .activitiesThisWeek(31)
                .totalDistanceKm(9876.5)
                .activitiesThisMonth(120)
                .build());

        mockMvc.perform(get("/api/v1/admin/stats")
                        .header("X-User-Id", "1")
                        .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(42))
                .andExpect(jsonPath("$.activeUsers").value(40))
                .andExpect(jsonPath("$.totalActivities").value(1234))
                .andExpect(jsonPath("$.totalClubs").value(3))
                .andExpect(jsonPath("$.totalChallenges").value(5))
                .andExpect(jsonPath("$.totalEvents").value(7))
                .andExpect(jsonPath("$.newUsersThisWeek").value(2))
                .andExpect(jsonPath("$.activitiesThisWeek").value(31))
                .andExpect(jsonPath("$.totalDistanceKm").value(9876.5))
                .andExpect(jsonPath("$.activitiesThisMonth").value(120));
    }

    @Test
    @DisplayName("Should forbid non-admin users")
    void shouldForbidNonAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats")
                        .header("X-User-Id", "1")
                        .header("X-User-Roles", "USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should reject unauthenticated requests")
    void shouldRejectUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats"))
                .andExpect(status().isForbidden());
    }
}
