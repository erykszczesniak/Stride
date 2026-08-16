package com.strava.webbff.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.webbff.client.ActivityClient;
import com.strava.webbff.client.AdminPage;
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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminStatsService Unit Tests")
class AdminStatsServiceTest {

    // Fixed "now": 2026-08-15T12:00:00Z -> week window starts 2026-08-08T12:00,
    // month window starts 2026-08-01T00:00.
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-15T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private UserClient userClient;
    @Mock
    private ActivityClient activityClient;
    @Mock
    private GoalClient goalClient;
    @Mock
    private EventClient eventClient;

    private AdminStatsService adminStatsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        adminStatsService = new AdminStatsService(userClient, activityClient, goalClient, eventClient, FIXED_CLOCK);
    }

    private JsonNode json(String content) {
        try {
            return objectMapper.readTree(content);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private AdminPage page(long totalElements, JsonNode... items) {
        return new AdminPage(List.of(items), 1, totalElements);
    }

    @Nested
    @DisplayName("Composition")
    class CompositionTests {

        @Test
        @DisplayName("Should compose totals from envelopes, scans and list sizes")
        void shouldComposeStats() {
            given(userClient.adminUsers(0, AdminStatsService.SCAN_PAGE_SIZE)).willReturn(page(3,
                    json("{\"active\":true,\"createdAt\":\"2026-08-14T10:00:00\"}"),   // this week + active
                    json("{\"active\":false,\"createdAt\":\"2026-08-12T10:00:00\"}"),  // this week, inactive
                    json("{\"active\":true,\"createdAt\":\"2026-07-01T10:00:00\"}"))); // old, active
            given(activityClient.adminActivities(0, AdminStatsService.SCAN_PAGE_SIZE)).willReturn(page(3,
                    json("{\"distance\":10.5,\"createdAt\":\"2026-08-14T18:00:00\"}"), // week + month
                    json("{\"distance\":4.5,\"createdAt\":\"2026-08-02T09:00:00\"}"),  // month only
                    json("{\"createdAt\":\"2026-07-10T09:00:00\"}")));                  // old, no distance
            given(eventClient.clubs(AdminStatsService.SCAN_PAGE_SIZE)).willReturn(List.of(json("{}")));
            given(goalClient.activeChallenges(AdminStatsService.SCAN_PAGE_SIZE))
                    .willReturn(List.of(json("{}"), json("{}")));
            given(eventClient.upcomingPublicEvents(AdminStatsService.SCAN_PAGE_SIZE))
                    .willReturn(List.of(json("{}"), json("{}"), json("{}")));

            AdminStatsResponse stats = adminStatsService.getAdminStats();

            assertThat(stats.getTotalUsers()).isEqualTo(3);
            assertThat(stats.getActiveUsers()).isEqualTo(2);
            assertThat(stats.getTotalActivities()).isEqualTo(3);
            assertThat(stats.getTotalClubs()).isEqualTo(1);
            assertThat(stats.getTotalChallenges()).isEqualTo(2);
            assertThat(stats.getTotalEvents()).isEqualTo(3);
            assertThat(stats.getNewUsersThisWeek()).isEqualTo(2);
            assertThat(stats.getActivitiesThisWeek()).isEqualTo(1);
            assertThat(stats.getActivitiesThisMonth()).isEqualTo(2);
            assertThat(stats.getTotalDistanceKm()).isEqualTo(15.0);
        }

        @Test
        @DisplayName("Should follow full scan pages and stop on a short page")
        void shouldScanAcrossPages() {
            List<JsonNode> fullPage = new ArrayList<>();
            IntStream.range(0, AdminStatsService.SCAN_PAGE_SIZE).forEach(i ->
                    fullPage.add(json("{\"active\":true,\"createdAt\":\"2026-08-14T10:00:00\"}")));
            given(userClient.adminUsers(0, AdminStatsService.SCAN_PAGE_SIZE))
                    .willReturn(new AdminPage(fullPage, 2, 201));
            given(userClient.adminUsers(1, AdminStatsService.SCAN_PAGE_SIZE)).willReturn(page(201,
                    json("{\"active\":false,\"createdAt\":\"2026-07-01T10:00:00\"}")));
            given(activityClient.adminActivities(0, AdminStatsService.SCAN_PAGE_SIZE))
                    .willReturn(AdminPage.EMPTY);
            given(eventClient.clubs(anyInt())).willReturn(List.of());
            given(goalClient.activeChallenges(anyInt())).willReturn(List.of());
            given(eventClient.upcomingPublicEvents(anyInt())).willReturn(List.of());

            AdminStatsResponse stats = adminStatsService.getAdminStats();

            assertThat(stats.getTotalUsers()).isEqualTo(201);
            assertThat(stats.getActiveUsers()).isEqualTo(200);
            assertThat(stats.getNewUsersThisWeek()).isEqualTo(200);
            then(userClient).should().adminUsers(1, AdminStatsService.SCAN_PAGE_SIZE);
        }
    }

    @Nested
    @DisplayName("Degradation")
    class DegradationTests {

        @Test
        @DisplayName("Should return zeros (and 0.0 distance, like main's COALESCE) when downstreams fall back")
        void shouldDegradeToZeros() {
            given(userClient.adminUsers(anyInt(), anyInt())).willReturn(AdminPage.EMPTY);
            given(activityClient.adminActivities(anyInt(), anyInt())).willReturn(AdminPage.EMPTY);
            given(eventClient.clubs(anyInt())).willReturn(List.of());
            given(goalClient.activeChallenges(anyInt())).willReturn(List.of());
            given(eventClient.upcomingPublicEvents(anyInt())).willReturn(List.of());

            AdminStatsResponse stats = adminStatsService.getAdminStats();

            assertThat(stats.getTotalUsers()).isZero();
            assertThat(stats.getActiveUsers()).isZero();
            assertThat(stats.getTotalActivities()).isZero();
            assertThat(stats.getTotalClubs()).isZero();
            assertThat(stats.getTotalChallenges()).isZero();
            assertThat(stats.getTotalEvents()).isZero();
            assertThat(stats.getNewUsersThisWeek()).isZero();
            assertThat(stats.getActivitiesThisWeek()).isZero();
            assertThat(stats.getActivitiesThisMonth()).isZero();
            assertThat(stats.getTotalDistanceKm()).isEqualTo(0.0);
        }
    }
}
