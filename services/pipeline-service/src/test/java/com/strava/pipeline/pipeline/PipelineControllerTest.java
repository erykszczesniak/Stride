package com.strava.pipeline.pipeline;

import com.strava.pipeline.exception.ApiRequestException;
import com.strava.shared.security.HeaderAuthenticationFilter.HeaderAuthenticationToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ported from main; the security context carries the branch's Long-principal token. */
@WebMvcTest(PipelineController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PipelineController")
class PipelineControllerTest {

    private static final long USER_ID = 7L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PipelineQueryService pipelineQueryService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(new HeaderAuthenticationToken(
                USER_ID, "user@test.dev", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static PipelineActivityDto sampleActivity() {
        return new PipelineActivityDto("act-1", "run", "Park Run",
                OffsetDateTime.parse("2026-07-11T08:00:00+02:00"), 1500, 5.0, 172, 186, true);
    }

    @Test
    @DisplayName("GET /pipeline/v1/activities returns the mapped list")
    void activities() throws Exception {
        given(pipelineQueryService.listActivities(USER_ID)).willReturn(List.of(sampleActivity()));

        mockMvc.perform(get("/pipeline/v1/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("act-1"))
                .andExpect(jsonPath("$[0].discipline").value("run"))
                .andExpect(jsonPath("$[0].hasGPS").value(true));
    }

    @Test
    @DisplayName("GET dashboard serializes a null todayWorkout (rest day) explicitly")
    void dashboardNullWorkout() throws Exception {
        PipelineDashboardDto dto = new PipelineDashboardDto(
                new PipelineDashboardDto.Form(OffsetDateTime.now(), 72, 64, 8),
                null,
                List.of(new PipelineDashboardDto.SportSnapshot("run", 42.3, -4)),
                List.of(sampleActivity()));
        given(pipelineQueryService.dashboard(USER_ID)).willReturn(dto);

        mockMvc.perform(get("/pipeline/v1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.form.ctl").value(72))
                .andExpect(jsonPath("$.todayWorkout").value(nullValue()))
                .andExpect(jsonPath("$.snapshots[0].trendPercent").value(-4));
    }

    @Test
    @DisplayName("records serialize the tagged-union value exactly as the contract nests it")
    void recordsValueUnion() throws Exception {
        PipelineRecordsDto dto = new PipelineRecordsDto(List.of(
                new PipelineRecordsDto.RecordItem(
                        "rec-1", "run", "5K",
                        PipelineRecordsDto.Value.ofSeconds(1152),
                        OffsetDateTime.parse("2026-06-28T00:00:00+02:00"),
                        true,
                        PipelineRecordsDto.Value.ofSeconds(1176),
                        List.of(1176.0, 1152.0))));
        given(pipelineQueryService.records(USER_ID)).willReturn(dto);

        mockMvc.perform(get("/pipeline/v1/records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].value.time.seconds").value(1152))
                .andExpect(jsonPath("$.records[0].value.power").doesNotExist())
                .andExpect(jsonPath("$.records[0].isNew").value(true))
                .andExpect(jsonPath("$.records[0].previousBest.time.seconds").value(1176));
    }

    @Test
    @DisplayName("unknown activity ids produce the contract's error body with 404")
    void notFoundErrorShape() throws Exception {
        given(pipelineQueryService.activityDetail(eq(USER_ID), any()))
                .willThrow(new ApiRequestException("Activity not found", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/pipeline/v1/activities/act-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"))
                .andExpect(jsonPath("$.message").value("Activity not found"));
    }

    @Test
    @DisplayName("downstream outages produce the contract's server_error body")
    void serverErrorShape() throws Exception {
        given(pipelineQueryService.dashboard(USER_ID))
                .willThrow(new ApiRequestException("Activity data unavailable", HttpStatus.INTERNAL_SERVER_ERROR));

        mockMvc.perform(get("/pipeline/v1/dashboard"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("server_error"))
                .andExpect(jsonPath("$.message").value("Activity data unavailable"));
    }

    @Test
    @DisplayName("load endpoint passes the range query through")
    void loadRange() throws Exception {
        given(pipelineQueryService.load(USER_ID, "3m")).willReturn(
                new PipelineLoadDto("3m", List.of(), List.of(), List.of(),
                        new PipelineLoadDto.WeekSummary(0, 0, 0, 0)));

        mockMvc.perform(get("/pipeline/v1/metrics/load").param("range", "3m"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.range").value("3m"));
    }
}
