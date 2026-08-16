package com.strava.webbff.report;

import com.strava.webbff.client.ActivityView;
import com.strava.webbff.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExportController.class)
@Import(SecurityConfig.class)
@DisplayName("ExportController Unit Tests")
class ExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    private static final ActivityView RUN = new ActivityView(
            1L, "Morning, Run", "RUN", 10.0, 3000, 120, 640, 12.0, 150,
            LocalDateTime.parse("2026-01-05T10:30:00"), LocalDateTime.parse("2026-01-05T11:00:00"), 3, 2);

    private static final ActivityView BARE = new ActivityView(
            2L, "Bare", "RIDE", null, null, null, null, null, null,
            null, LocalDateTime.parse("2026-01-06T09:00:00"), 0, 0);

    @Test
    @DisplayName("Should stream the CSV with main's exact header row, values and Content-Disposition")
    void shouldExportCsv() throws Exception {
        given(reportService.activitiesFor(eq("2026-01-01"), eq("2026-01-31"), isNull()))
                .willReturn(List.of(RUN, BARE));

        mockMvc.perform(post("/api/v1/export/report/csv")
                        .header("X-User-Id", "1")
                        .header("X-User-Roles", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dateFrom":"2026-01-01","dateTo":"2026-01-31",
                                 "fields":["activityTitle","activityType","date","distance","duration","pace"],
                                 "activityType":null,"units":"metric"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/octet-stream"))
                .andExpect(header().string("Content-Disposition",
                        "form-data; name=\"attachment\"; filename=\"report.csv\""))
                // Titles are quoted; missing distance renders "0.0" (main's int-vs-Double
                // ternary promotion); missing startTime renders an empty date; missing
                // duration renders "0s"; pace falls back to "-".
                .andExpect(content().string(
                        "activityTitle,activityType,date,distance,duration,pace\n"
                                + "\"Morning, Run\",RUN,2026-01-05 10:30,10.0,50m 0s,5:00 min/km\n"
                                + "\"Bare\",RIDE,,0.0,0s,-\n"));
    }

    @Test
    @DisplayName("Should format pace with imperial units when requested")
    void shouldExportImperialPace() throws Exception {
        given(reportService.activitiesFor(eq("2026-01-01"), eq("2026-01-31"), eq("RUN")))
                .willReturn(List.of(RUN));

        mockMvc.perform(post("/api/v1/export/report/csv")
                        .header("X-User-Id", "1")
                        .header("X-User-Roles", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dateFrom":"2026-01-01","dateTo":"2026-01-31",
                                 "fields":["pace"],"activityType":"RUN","units":"imperial"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("pace\n8:03 min/mi\n"));
    }

    @Test
    @DisplayName("Should reject unauthenticated exports")
    void shouldRejectUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/export/report/csv")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
