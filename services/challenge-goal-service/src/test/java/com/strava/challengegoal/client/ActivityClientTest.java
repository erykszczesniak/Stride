package com.strava.challengegoal.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/**
 * Verifies parsing of activity-service's /api/v1 contract: a plain JSON array
 * of ActivityResponse objects (not a Spring Page envelope), summed client-side
 * with sport + date-window filtering.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityClient Unit Tests")
class ActivityClientTest {

    @Mock
    private RestTemplate loadBalanced;

    @InjectMocks
    private ActivityClient activityClient;

    private static final String URL = "lb://activity-service/api/v1/activities/user/7?size=200&page=0";

    private static Map<String, Object> activity(String type, String startTime,
                                                Double distance, Integer duration, Integer elevation) {
        Map<String, Object> a = new HashMap<>();
        a.put("activityType", type);
        a.put("startTime", startTime);
        a.put("distance", distance);
        a.put("duration", duration);
        a.put("elevationGain", elevation);
        return a;
    }

    @SuppressWarnings("unchecked")
    private void givenBody(List<Map<String, Object>> body) {
        given(loadBalanced.exchange(eq(URL), eq(HttpMethod.GET), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .willReturn(ResponseEntity.ok(body));
    }

    @Test
    @DisplayName("Should sum activities from the JSON array, filtering sport and window")
    void shouldSumActivitiesFilteringSportAndWindow() {
        LocalDateTime from = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 8, 31, 23, 59);
        givenBody(List.of(
                activity("RUN", "2026-08-10T08:00:00", 10.0, 3600, 100),
                activity("RUN", "2026-08-12T08:00:00", 5.0, 1800, 50),
                activity("RIDE", "2026-08-13T08:00:00", 40.0, 7200, 300),   // wrong sport
                activity("RUN", "2026-07-01T08:00:00", 21.0, 7200, 200)    // outside window
        ));

        ActivitySummary summary = activityClient.summaryFor(7L, "RUN", from, to);

        assertThat(summary.totalDistanceKm()).isEqualTo(15.0);
        assertThat(summary.totalDurationSeconds()).isEqualTo(5400);
        assertThat(summary.totalElevationMeters()).isEqualTo(150.0);
        assertThat(summary.activityCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should include all sports when no sport filter is set")
    void shouldIncludeAllSportsWhenNoFilter() {
        LocalDateTime from = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 8, 31, 23, 59);
        givenBody(List.of(
                activity("RUN", "2026-08-10T08:00:00", 10.0, 3600, 100),
                activity("RIDE", "2026-08-13T08:00:00", 40.0, 7200, 300)
        ));

        ActivitySummary summary = activityClient.summaryFor(7L, null, from, to);

        assertThat(summary.totalDistanceKm()).isEqualTo(50.0);
        assertThat(summary.activityCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return EMPTY when the body is null")
    void shouldReturnEmptyWhenBodyIsNull() {
        givenBody(null);

        ActivitySummary summary = activityClient.summaryFor(7L, "RUN",
                LocalDateTime.now().minusDays(7), LocalDateTime.now());

        assertThat(summary).isEqualTo(ActivitySummary.EMPTY);
    }
}
