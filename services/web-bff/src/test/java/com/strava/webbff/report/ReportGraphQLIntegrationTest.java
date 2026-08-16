package com.strava.webbff.report;

import com.strava.webbff.client.ActivityClient;
import com.strava.webbff.client.ActivityView;
import com.strava.webbff.client.EventClient;
import com.strava.webbff.client.GoalClient;
import com.strava.webbff.client.UserClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-context test of the /graphql transport: boots the application (schema
 * wiring, security chain, Resilience4j aspects) and posts exactly the JSON
 * body Reports.jsx sends — operation GenerateReport with dateFrom/dateTo/
 * fields/activityType variables.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("GenerateReport GraphQL Integration Tests")
class ReportGraphQLIntegrationTest {

    private static final String QUERY = """
            query GenerateReport($dateFrom: String!, $dateTo: String!, $fields: [String!]!, $activityType: String) {
              generateReport(dateFrom: $dateFrom, dateTo: $dateTo, fields: $fields, activityType: $activityType) {
                rows { activityTitle date distance duration pace likesCount commentsCount }
                summary {
                  totalDistance totalDuration totalCalories totalElevation totalActivities
                  avgDistance avgDuration avgCalories
                }
              }
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserClient userClient;
    @MockitoBean
    private ActivityClient activityClient;
    @MockitoBean
    private GoalClient goalClient;
    @MockitoBean
    private EventClient eventClient;

    @Test
    @DisplayName("Should answer the frontend's GenerateReport operation with rows and summary")
    void shouldGenerateReport() throws Exception {
        given(activityClient.allMyActivities()).willReturn(List.of(
                new ActivityView(1L, "Morning Run", "RUN", 10.0, 3000, 100, 500, 12.0, 150,
                        LocalDateTime.parse("2026-01-05T10:30:00"), LocalDateTime.parse("2026-01-05T11:00:00"), 3, 2),
                new ActivityView(2L, "Evening Run", "RUN", 5.5, 1800, 50, 300, 11.0, 145,
                        LocalDateTime.parse("2026-01-10T19:00:00"), LocalDateTime.parse("2026-01-10T20:00:00"), 1, 0),
                new ActivityView(3L, "Outside window", "RUN", 7.0, 2100, 70, 400, 12.0, 150,
                        LocalDateTime.parse("2026-02-05T10:00:00"), LocalDateTime.parse("2026-02-05T11:00:00"), 0, 0)));

        String body = """
                {"query": %s,
                 "variables": {"dateFrom": "2026-01-01", "dateTo": "2026-01-31",
                               "fields": ["activityTitle", "date", "distance", "duration", "pace",
                                          "likesCount", "commentsCount"],
                               "activityType": null}}
                """.formatted(com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .writeValueAsString(QUERY));

        mockMvc.perform(post("/graphql")
                        .header("X-User-Id", "1")
                        .header("X-User-Email", "jan@stride.app")
                        .header("X-User-Roles", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.generateReport.rows.length()").value(2))
                .andExpect(jsonPath("$.data.generateReport.rows[0].activityTitle").value("Morning Run"))
                .andExpect(jsonPath("$.data.generateReport.rows[0].date").value("2026-01-05 10:30"))
                .andExpect(jsonPath("$.data.generateReport.rows[0].pace").value("5:00 min/km"))
                .andExpect(jsonPath("$.data.generateReport.rows[0].likesCount").value(3))
                .andExpect(jsonPath("$.data.generateReport.rows[1].activityTitle").value("Evening Run"))
                .andExpect(jsonPath("$.data.generateReport.summary.totalDistance").value(15.5))
                .andExpect(jsonPath("$.data.generateReport.summary.totalDuration").value(4800))
                .andExpect(jsonPath("$.data.generateReport.summary.totalCalories").value(800))
                .andExpect(jsonPath("$.data.generateReport.summary.totalElevation").value(150))
                .andExpect(jsonPath("$.data.generateReport.summary.totalActivities").value(2))
                .andExpect(jsonPath("$.data.generateReport.summary.avgDistance").value(7.8))
                .andExpect(jsonPath("$.data.generateReport.summary.avgDuration").value(2400))
                .andExpect(jsonPath("$.data.generateReport.summary.avgCalories").value(400));
    }

    @Test
    @DisplayName("Should filter by activityType when the wizard selects a sport")
    void shouldFilterByActivityType() throws Exception {
        given(activityClient.allMyActivities()).willReturn(List.of(
                new ActivityView(1L, "Run", "RUN", 10.0, 3000, 100, 500, 12.0, 150,
                        LocalDateTime.parse("2026-01-05T10:30:00"), LocalDateTime.parse("2026-01-05T11:00:00"), 0, 0),
                new ActivityView(2L, "Ride", "RIDE", 35.0, 5400, 300, 900, 23.3, 140,
                        LocalDateTime.parse("2026-01-06T10:30:00"), LocalDateTime.parse("2026-01-06T12:00:00"), 0, 0)));

        String body = """
                {"query": %s,
                 "variables": {"dateFrom": "2026-01-01", "dateTo": "2026-01-31",
                               "fields": ["activityTitle", "pace"], "activityType": "RIDE"}}
                """.formatted(com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .writeValueAsString(QUERY));

        mockMvc.perform(post("/graphql")
                        .header("X-User-Id", "1")
                        .header("X-User-Roles", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.generateReport.rows.length()").value(1))
                .andExpect(jsonPath("$.data.generateReport.rows[0].activityTitle").value("Ride"))
                .andExpect(jsonPath("$.data.generateReport.rows[0].pace").value("23.3 km/h"));
    }

    @Test
    @DisplayName("Should reject unauthenticated /graphql posts")
    void shouldRejectUnauthenticated() throws Exception {
        mockMvc.perform(post("/graphql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"{ __typename }\"}"))
                .andExpect(status().isForbidden());
    }
}
