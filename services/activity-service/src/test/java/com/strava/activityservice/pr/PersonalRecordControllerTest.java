package com.strava.activityservice.pr;

import com.strava.activityservice.config.SecurityConfig;
import com.strava.activityservice.domain.ActivityType;
import com.strava.activityservice.domain.DistanceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PersonalRecordController.class)
@Import(SecurityConfig.class)
@DisplayName("PersonalRecordController Unit Tests")
class PersonalRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PersonalRecordMapper personalRecordMapper;

    private PersonalRecordResponse sampleResponse() {
        return PersonalRecordResponse.builder()
                .id(1L)
                .activityType(ActivityType.RUN)
                .distanceType(DistanceType.FIVE_KM)
                .distance(5.0)
                .timeInSeconds(1500)
                .formattedTime("25:00")
                .formattedPace("5:00 /km")
                .speedKmh(12.0)
                .activityId(4L)
                .activityTitle("Interval Training")
                .previousTimeInSeconds(1694)
                .improvementSeconds(194)
                .improvementPercent(11.5)
                .build();
    }

    @Test
    @DisplayName("GET / returns the caller's PRs")
    void getMyPRs() throws Exception {
        when(personalRecordMapper.getUserPRs(1L)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/personal-records").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].distanceType").value("FIVE_KM"))
                .andExpect(jsonPath("$[0].formattedTime").value("25:00"))
                .andExpect(jsonPath("$[0].activityTitle").value("Interval Training"))
                .andExpect(jsonPath("$[0].improvementSeconds").value(194));
    }

    @Test
    @DisplayName("GET /user/{userId} is privacy-aware via the mapper")
    void getUserPRs() throws Exception {
        when(personalRecordMapper.getUserPRsVisibleTo(2L, 1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/personal-records/user/2").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(personalRecordMapper).getUserPRsVisibleTo(2L, 1L);
    }

    @Test
    @DisplayName("GET /activity-type/{type} filters by sport")
    void getByActivityType() throws Exception {
        when(personalRecordMapper.getUserPRsByActivityType(1L, ActivityType.RUN))
                .thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/personal-records/activity-type/RUN").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].activityType").value("RUN"));
    }

    @Test
    @DisplayName("POST /recalculate answers with main's message body")
    void recalculate() throws Exception {
        mockMvc.perform(post("/api/v1/personal-records/recalculate").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Personal records recalculated successfully"));

        verify(personalRecordMapper).recalculateAllPRs(1L);
    }
}
