package com.strava.activityservice.internal;

import com.strava.activityservice.activity.ActivityService;
import com.strava.activityservice.config.SecurityConfig;
import com.strava.activityservice.domain.Activity;
import com.strava.activityservice.domain.ActivityRepository;
import com.strava.activityservice.util.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalActivityController.class)
@Import(SecurityConfig.class)
@DisplayName("InternalActivityController Unit Tests")
class InternalActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivityRepository activityRepository;

    @MockitoBean
    private ActivityService activityService;

    @Test
    @DisplayName("/internal/** is reachable without identity headers (cluster-only surface)")
    void internalNeedsNoIdentityHeaders() throws Exception {
        when(activityRepository.findAllById(List.of(1L))).thenReturn(List.of());

        mockMvc.perform(get("/internal/activities").param("ids", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("batch payload carries the contract fields; missing ids are silently skipped")
    void batchPayloadShape() throws Exception {
        Activity activity = TestDataFactory.createActivity(1L, 5L);
        activity.setPrivate(true);
        activity.setRoutePolyline("abc");
        activity.setElevationProfile("[1.0,2.0]");
        activity.setPointTimestamps("[100,200]");
        activity.setAverageHeartRate(150);
        activity.setAveragePower(210);
        activity.setCalories(600);
        activity.setStartLocation("Warsaw");
        activity.setPhotos(List.of("/uploads/photos/a.jpg"));
        when(activityRepository.findAllById(List.of(1L, 999L))).thenReturn(List.of(activity));

        mockMvc.perform(get("/internal/activities").param("ids", "1,999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].userId").value(5))
                .andExpect(jsonPath("$[0].isPrivate").value(true))
                .andExpect(jsonPath("$[0].routePolyline").value("abc"))
                .andExpect(jsonPath("$[0].elevationProfile[1]").value(2.0))
                .andExpect(jsonPath("$[0].pointTimestamps[0]").value(100))
                .andExpect(jsonPath("$[0].averageHeartRate").value(150))
                .andExpect(jsonPath("$[0].averagePower").value(210))
                .andExpect(jsonPath("$[0].calories").value(600))
                .andExpect(jsonPath("$[0].startLocation").value("Warsaw"))
                .andExpect(jsonPath("$[0].photos[0]").value("/uploads/photos/a.jpg"))
                // no social counts, no embedded user object on the internal payload
                .andExpect(jsonPath("$[0].likesCount").doesNotExist())
                .andExpect(jsonPath("$[0].user").doesNotExist());
    }

    @Test
    @DisplayName("stats/range returns the contract keys")
    void statsRangeShape() throws Exception {
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 1, 31, 23, 59, 59);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalDistanceKm", 120.5);
        stats.put("totalDurationSeconds", 36000L);
        stats.put("totalElevationGain", 900L);
        stats.put("totalCalories", 8000L);
        stats.put("activityCount", 12);
        when(activityService.getRangeStats(eq(7L), eq(from), eq(to))).thenReturn(stats);

        mockMvc.perform(get("/internal/activities/stats/range")
                        .param("userId", "7")
                        .param("from", "2026-01-01T00:00:00")
                        .param("to", "2026-01-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDistanceKm").value(120.5))
                .andExpect(jsonPath("$.totalDurationSeconds").value(36000))
                .andExpect(jsonPath("$.totalElevationGain").value(900))
                .andExpect(jsonPath("$.totalCalories").value(8000))
                .andExpect(jsonPath("$.activityCount").value(12));
    }
}
