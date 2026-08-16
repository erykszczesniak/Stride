package com.strava.webbff.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.webbff.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
@DisplayName("DashboardController Unit Tests")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        given(dashboardService.getDashboardData()).willReturn(new DashboardResponse(
                objectMapper.readTree("{\"id\":1,\"firstName\":\"Jan\",\"fullName\":\"Jan Kowalski\"}"),
                objectMapper.readTree("{\"totalDistance\":42.5,\"totalActivities\":7}"),
                objectMapper.readTree("{\"totalDistance\":12.0,\"dailyStats\":[]}"),
                List.of(objectMapper.readTree("{\"id\":11,\"title\":\"Morning Run\",\"likesCount\":3}")),
                null,
                List.of(objectMapper.readTree("{\"id\":2,\"isRegistered\":true,\"registeredCount\":9}"))));
    }

    @Test
    @DisplayName("Should return the composed dashboard with main's exact section names")
    void shouldReturnDashboard() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard")
                        .header("X-User-Id", "1")
                        .header("X-User-Email", "jan@stride.app")
                        .header("X-User-Roles", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.fullName").value("Jan Kowalski"))
                .andExpect(jsonPath("$.stats.totalDistance").value(42.5))
                .andExpect(jsonPath("$.weeklyStats.dailyStats").isArray())
                .andExpect(jsonPath("$.recentActivities[0].title").value("Morning Run"))
                .andExpect(jsonPath("$.weeklyGoal").value(nullValue()))
                .andExpect(jsonPath("$.upcomingEvents[0].isRegistered").value(true));
    }

    @Test
    @DisplayName("Should reject unauthenticated requests (no X-User-Id header)")
    void shouldRejectUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should not map GET /api/v1/dashboard/stats — main 404s there too")
    void shouldNotMapDashboardStats() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/stats")
                        .header("X-User-Id", "1")
                        .header("X-User-Roles", "USER"))
                .andExpect(status().isNotFound());
    }
}
