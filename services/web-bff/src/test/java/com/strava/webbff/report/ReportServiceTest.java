package com.strava.webbff.report;

import com.strava.webbff.client.ActivityClient;
import com.strava.webbff.client.ActivityView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportService Unit Tests")
class ReportServiceTest {

    @Mock
    private ActivityClient activityClient;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(activityClient);
    }

    private static ActivityView activity(String title, String type, Double distance, Integer duration,
                                         Integer elevation, Integer calories, LocalDateTime startTime) {
        return new ActivityView(1L, title, type, distance, duration, elevation, calories,
                12.5, 150, startTime, LocalDateTime.of(2026, 1, 2, 8, 0), 3, 2);
    }

    @Nested
    @DisplayName("activitiesFor — date range and type filtering")
    class FilteringTests {

        @Test
        @DisplayName("Should keep activities inside the inclusive dateFrom..dateTo window only")
        void shouldFilterByDateRangeInclusive() {
            given(activityClient.allMyActivities()).willReturn(List.of(
                    activity("start edge", "RUN", 5.0, 1500, 10, 200, LocalDateTime.parse("2026-01-01T00:00:00")),
                    activity("end edge", "RUN", 5.0, 1500, 10, 200, LocalDateTime.parse("2026-01-31T23:59:59")),
                    activity("before", "RUN", 5.0, 1500, 10, 200, LocalDateTime.parse("2025-12-31T23:59:59")),
                    activity("after", "RUN", 5.0, 1500, 10, 200, LocalDateTime.parse("2026-02-01T00:00:00")),
                    activity("no start time", "RUN", 5.0, 1500, 10, 200, null)));

            List<ActivityView> result = reportService.activitiesFor("2026-01-01", "2026-01-31", null);

            assertThat(result).extracting(ActivityView::title)
                    .containsExactly("start edge", "end edge");
        }

        @Test
        @DisplayName("Should filter by activity type when provided")
        void shouldFilterByActivityType() {
            given(activityClient.allMyActivities()).willReturn(List.of(
                    activity("run", "RUN", 5.0, 1500, 10, 200, LocalDateTime.parse("2026-01-10T10:00:00")),
                    activity("ride", "RIDE", 30.0, 5400, 100, 700, LocalDateTime.parse("2026-01-11T10:00:00"))));

            List<ActivityView> result = reportService.activitiesFor("2026-01-01", "2026-01-31", "RUN");

            assertThat(result).extracting(ActivityView::title).containsExactly("run");
        }

        @Test
        @DisplayName("Should reject an unknown activity type exactly like main's valueOf")
        void shouldRejectUnknownActivityType() {
            assertThatThrownBy(() -> reportService.activitiesFor("2026-01-01", "2026-01-31", "BOGUS"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("buildRow — field derivations")
    class RowTests {

        @Test
        @DisplayName("Should emit only the requested fields, skipping unknown ones")
        void shouldBuildRequestedFieldsOnly() {
            ActivityView a = activity("Morning Run", "RUN", 10.0, 3000, 120, 640,
                    LocalDateTime.parse("2026-01-05T10:30:00"));

            Map<String, Object> row = reportService.buildRow(a,
                    List.of("activityTitle", "date", "pace", "likesCount", "commentsCount", "bogus"));

            assertThat(row).containsExactly(
                    Map.entry("activityTitle", "Morning Run"),
                    Map.entry("date", "2026-01-05 10:30"),
                    Map.entry("pace", "5:00 min/km"),
                    Map.entry("likesCount", 3),
                    Map.entry("commentsCount", 2));
        }

        @Test
        @DisplayName("Should fall back to createdAt for the date when startTime is missing")
        void shouldFallBackToCreatedAt() {
            ActivityView a = activity("Legacy", "RUN", 10.0, 3000, 120, 640, null);

            Map<String, Object> row = reportService.buildRow(a, List.of("date"));

            assertThat(row).containsEntry("date", "2026-01-02 08:00");
        }
    }

    @Nested
    @DisplayName("buildSummary — totals and averages")
    class SummaryTests {

        @Test
        @DisplayName("Should total and average with main's x10 rounding")
        void shouldComputeSummary() {
            List<ActivityView> activities = List.of(
                    activity("a", "RUN", 10.0, 3000, 100, 500, LocalDateTime.parse("2026-01-05T10:00:00")),
                    activity("b", "RUN", 5.5, 1800, 50, 300, LocalDateTime.parse("2026-01-06T10:00:00")),
                    activity("c", "RUN", null, null, null, null, LocalDateTime.parse("2026-01-07T10:00:00")));

            Map<String, Object> summary = reportService.buildSummary(activities);

            assertThat(summary)
                    .containsEntry("totalDistance", 15.5)
                    .containsEntry("totalDuration", 4800)
                    .containsEntry("totalCalories", 800)
                    .containsEntry("totalElevation", 150)
                    .containsEntry("totalActivities", 3)
                    // 15.5 / 3 = 5.1666 -> 5.2 (rounded to one decimal, like main)
                    .containsEntry("avgDistance", 5.2)
                    .containsEntry("avgDuration", 1600)
                    .containsEntry("avgCalories", 266);
        }

        @Test
        @DisplayName("Should return zeros for an empty report window")
        void shouldReturnZerosWhenEmpty() {
            Map<String, Object> summary = reportService.buildSummary(List.of());

            assertThat(summary)
                    .containsEntry("totalDistance", 0.0)
                    .containsEntry("totalDuration", 0)
                    .containsEntry("totalCalories", 0)
                    .containsEntry("totalElevation", 0)
                    .containsEntry("totalActivities", 0)
                    .containsEntry("avgDistance", 0.0)
                    .containsEntry("avgDuration", 0)
                    .containsEntry("avgCalories", 0);
        }
    }
}
