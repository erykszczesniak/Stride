package com.strava.webbff.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.webbff.client.ActivityClient;
import com.strava.webbff.client.EventClient;
import com.strava.webbff.client.GoalClient;
import com.strava.webbff.client.UserClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService Unit Tests")
class DashboardServiceTest {

    @Mock
    private UserClient userClient;
    @Mock
    private ActivityClient activityClient;
    @Mock
    private GoalClient goalClient;
    @Mock
    private EventClient eventClient;

    private DashboardService dashboardService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(userClient, activityClient, goalClient, eventClient);
    }

    private JsonNode json(String content) {
        try {
            return objectMapper.readTree(content);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Nested
    @DisplayName("Composition")
    class CompositionTests {

        @Test
        @DisplayName("Should compose all six sections verbatim from the owning services")
        void shouldComposeAllSections() {
            given(userClient.currentUser()).willReturn(json("{\"id\":1,\"firstName\":\"Jan\",\"followersCount\":4}"));
            given(activityClient.monthlyStats()).willReturn(json("{\"totalDistance\":42.5}"));
            given(activityClient.weeklyStats()).willReturn(json("{\"totalDistance\":12.0,\"dailyStats\":[]}"));
            given(activityClient.recentActivities(5)).willReturn(List.of(
                    json("{\"id\":11,\"title\":\"Run A\"}"), json("{\"id\":10,\"title\":\"Run B\"}")));
            given(goalClient.activeGoals()).willReturn(List.of(
                    json("{\"id\":5,\"period\":\"MONTHLY\"}"), json("{\"id\":7,\"period\":\"WEEKLY\"}")));
            given(eventClient.myUpcomingEvents()).willReturn(List.of(
                    json("{\"id\":1}"), json("{\"id\":2}"), json("{\"id\":3}"), json("{\"id\":4}")));

            DashboardResponse response = dashboardService.getDashboardData();

            assertThat(response.user().path("firstName").asText()).isEqualTo("Jan");
            assertThat(response.stats().path("totalDistance").asDouble()).isEqualTo(42.5);
            assertThat(response.weeklyStats().path("dailyStats").isArray()).isTrue();
            assertThat(response.recentActivities()).hasSize(2);
            assertThat(response.recentActivities().get(0).path("title").asText()).isEqualTo("Run A");
            // The WEEKLY goal is picked, MONTHLY skipped — main's getActiveGoalByPeriod(userId, WEEKLY)
            assertThat(response.weeklyGoal().path("id").asInt()).isEqualTo(7);
            // Next 3 events only, order preserved — main's getUpcomingEventsByUserId(userId, 3)
            assertThat(response.upcomingEvents()).extracting(e -> e.path("id").asInt())
                    .containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("Should leave weeklyGoal null when no WEEKLY goal is active")
        void shouldLeaveWeeklyGoalNull() {
            given(userClient.currentUser()).willReturn(json("{\"id\":1}"));
            given(activityClient.monthlyStats()).willReturn(json("{}"));
            given(activityClient.weeklyStats()).willReturn(json("{}"));
            given(activityClient.recentActivities(5)).willReturn(List.of());
            given(goalClient.activeGoals()).willReturn(List.of(json("{\"id\":5,\"period\":\"MONTHLY\"}")));
            given(eventClient.myUpcomingEvents()).willReturn(List.of());

            DashboardResponse response = dashboardService.getDashboardData();

            assertThat(response.weeklyGoal()).isNull();
        }
    }

    @Nested
    @DisplayName("Degradation")
    class DegradationTests {

        @Test
        @DisplayName("Should degrade sections to null/empty when downstreams fall back")
        void shouldDegradeGracefully() throws Exception {
            // Client fallbacks return null / empty lists — the dashboard must still render.
            given(userClient.currentUser()).willReturn(null);
            given(activityClient.monthlyStats()).willReturn(null);
            given(activityClient.weeklyStats()).willReturn(null);
            given(activityClient.recentActivities(5)).willReturn(List.of());
            given(goalClient.activeGoals()).willReturn(List.of());
            given(eventClient.myUpcomingEvents()).willReturn(List.of());

            DashboardResponse response = dashboardService.getDashboardData();

            assertThat(response.user()).isNull();
            assertThat(response.stats()).isNull();
            assertThat(response.weeklyStats()).isNull();
            assertThat(response.recentActivities()).isEmpty();
            assertThat(response.weeklyGoal()).isNull();
            assertThat(response.upcomingEvents()).isEmpty();

            // Null sections serialize as JSON null, exactly like main's Lombok DTO.
            String serialized = objectMapper.writeValueAsString(response);
            assertThat(serialized).isEqualTo(
                    "{\"user\":null,\"stats\":null,\"weeklyStats\":null,"
                            + "\"recentActivities\":[],\"weeklyGoal\":null,\"upcomingEvents\":[]}");
        }
    }
}
