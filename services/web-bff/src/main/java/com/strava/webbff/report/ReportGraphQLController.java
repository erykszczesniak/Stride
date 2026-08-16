package com.strava.webbff.report;

import com.strava.webbff.client.ActivityView;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

/**
 * POST /graphql — the report wizard's {@code GenerateReport} query, the one
 * GraphQL operation any client actually uses (see docs/parity-plan.md). The
 * schema exposes exactly the report types; identity reaches activity-service
 * via the forwarded X-User-* headers, so the fetched history is the caller's
 * own — same effect as main's {@code getAuthenticatedUserId()} + repository
 * filter.
 */
@Controller
@RequiredArgsConstructor
public class ReportGraphQLController {

    private final ReportService reportService;

    @QueryMapping
    public Map<String, Object> generateReport(
            @Argument String dateFrom,
            @Argument String dateTo,
            @Argument List<String> fields,
            @Argument String activityType) {

        List<ActivityView> activities = reportService.activitiesFor(dateFrom, dateTo, activityType);

        List<Map<String, Object>> rows = activities.stream()
                .map(a -> reportService.buildRow(a, fields))
                .toList();

        Map<String, Object> summary = reportService.buildSummary(activities);

        return Map.of("rows", rows, "summary", summary);
    }
}
