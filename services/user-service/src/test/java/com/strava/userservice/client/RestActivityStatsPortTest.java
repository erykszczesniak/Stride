package com.strava.userservice.client;

import com.strava.userservice.user.ActivityStatsPort.ActivityTotals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestActivityStatsPortTest {

    @Mock
    private RestTemplate restTemplate;

    private RestActivityStatsPort port;

    @BeforeEach
    void setUp() {
        port = new RestActivityStatsPort(restTemplate);
    }

    @Test
    void totalsMapInternalStatsResponse() {
        Map<String, Object> body = Map.of(
                "activityCount", 7,
                "totalDistanceKm", 123.5,
                "totalDurationSeconds", 3600,
                "totalElevationGain", 250,
                "totalCalories", 4200);
        when(restTemplate.exchange(contains("/internal/activities/stats/range"), eq(HttpMethod.GET),
                any(HttpEntity.class), any(ParameterizedTypeReference.class), eq(9L)))
                .thenReturn(ResponseEntity.ok(body));

        ActivityTotals totals = port.totalsFor(9L);

        assertThat(totals.totalActivities()).isEqualTo(7);
        assertThat(totals.totalDistance()).isEqualTo(123.5);
        assertThat(totals.totalDurationSeconds()).isEqualTo(3600L);
        assertThat(totals.totalElevationMeters()).isEqualTo(250L);
        assertThat(totals.totalCalories()).isEqualTo(4200L);
    }

    @Test
    void totalsWithNullBodyDegradeToEmpty() {
        when(restTemplate.exchange(contains("/internal/activities/stats/range"), eq(HttpMethod.GET),
                any(HttpEntity.class), any(ParameterizedTypeReference.class), eq(9L)))
                .thenReturn(ResponseEntity.ok(null));

        assertThat(port.totalsFor(9L)).isEqualTo(ActivityTotals.empty());
    }

    @Test
    void recentActivitiesForwardViewerIdHeader() {
        when(restTemplate.exchange(contains("/api/v1/activities/user/{userId}"), eq(HttpMethod.GET),
                any(HttpEntity.class), any(ParameterizedTypeReference.class), eq(9L)))
                .thenAnswer(inv -> {
                    HttpEntity<?> entity = inv.getArgument(2);
                    assertThat(entity.getHeaders().getFirst("X-User-Id")).isEqualTo("4");
                    return ResponseEntity.ok(List.of(Map.of("id", 1)));
                });

        List<Object> recent = port.recentPublicActivities(9L, 4L);

        assertThat(recent).hasSize(1);
    }

    @Test
    void recentActivitiesOmitHeaderForAnonymousViewer() {
        when(restTemplate.exchange(contains("/api/v1/activities/user/{userId}"), eq(HttpMethod.GET),
                any(HttpEntity.class), any(ParameterizedTypeReference.class), eq(9L)))
                .thenAnswer(inv -> {
                    HttpEntity<?> entity = inv.getArgument(2);
                    assertThat(entity.getHeaders().containsKey("X-User-Id")).isFalse();
                    return ResponseEntity.ok(List.of());
                });

        assertThat(port.recentPublicActivities(9L, null)).isEmpty();
    }
}
