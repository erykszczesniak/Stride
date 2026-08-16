package com.strava.webbff.report;

import com.strava.webbff.client.ActivityClient;
import com.strava.webbff.client.ActivityView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The report builder's shared core, ported from the monolith's
 * {@code ReportGraphQLController} (row/summary derivations) — the CSV export
 * reuses the same activity selection, exactly like main queried the same
 * repository methods from both places.
 *
 * <p>Selection mirrors main's
 * {@code findByUserId[AndActivityType]AndDateRange}: {@code startTime BETWEEN
 * dateFrom 00:00:00 AND dateTo 23:59:59} (inclusive; activities without a
 * startTime never match, as with SQL BETWEEN), optional exact activity-type
 * match validated via {@code ActivityType.valueOf} like main.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final DateTimeFormatter ROW_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ActivityClient activityClient;

    public List<ActivityView> activitiesFor(String dateFrom, String dateTo, String activityType) {
        LocalDateTime from = LocalDateTime.parse(dateFrom + "T00:00:00");
        LocalDateTime to = LocalDateTime.parse(dateTo + "T23:59:59");
        ActivityType type = (activityType != null && !activityType.isEmpty())
                ? ActivityType.valueOf(activityType)
                : null;

        return activityClient.allMyActivities().stream()
                .filter(a -> a.startTime() != null)
                .filter(a -> !a.startTime().isBefore(from) && !a.startTime().isAfter(to))
                .filter(a -> type == null || type.name().equals(a.activityType()))
                .toList();
    }

    /** Ported 1:1 from main's buildRow — unknown fields are silently skipped. */
    public Map<String, Object> buildRow(ActivityView a, List<String> fields) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (String field : fields) {
            switch (field) {
                case "activityTitle" -> row.put("activityTitle", a.title());
                case "activityType" -> row.put("activityType", a.activityType());
                case "date" -> row.put("date", a.startTime() != null
                        ? a.startTime().format(ROW_DATE_FORMAT)
                        : a.createdAt().format(ROW_DATE_FORMAT));
                case "distance" -> row.put("distance", a.distance());
                case "duration" -> row.put("duration", a.duration());
                case "elevationGain" -> row.put("elevationGain", a.elevationGain());
                case "calories" -> row.put("calories", a.calories());
                case "averageSpeed" -> row.put("averageSpeed", a.averageSpeed());
                case "averageHeartRate" -> row.put("averageHeartRate", a.averageHeartRate());
                case "pace" -> row.put("pace",
                        PaceFormatter.format(typeOf(a), a.distance(), a.duration(), false));
                case "likesCount" -> row.put("likesCount", a.likesCount());
                case "commentsCount" -> row.put("commentsCount", a.commentsCount());
                default -> {
                }
            }
        }
        return row;
    }

    /** Ported 1:1 from main's buildSummary, including the x10 rounding. */
    public Map<String, Object> buildSummary(List<ActivityView> activities) {
        double totalDistance = activities.stream().mapToDouble(a -> a.distance() != null ? a.distance() : 0).sum();
        int totalDuration = activities.stream().mapToInt(a -> a.duration() != null ? a.duration() : 0).sum();
        int totalCalories = activities.stream().mapToInt(a -> a.calories() != null ? a.calories() : 0).sum();
        int totalElevation = activities.stream().mapToInt(a -> a.elevationGain() != null ? a.elevationGain() : 0).sum();
        int count = activities.size();

        return Map.of(
                "totalDistance", Math.round(totalDistance * 10.0) / 10.0,
                "totalDuration", totalDuration,
                "totalCalories", totalCalories,
                "totalElevation", totalElevation,
                "totalActivities", count,
                "avgDistance", count > 0 ? Math.round((totalDistance / count) * 10.0) / 10.0 : 0.0,
                "avgDuration", count > 0 ? totalDuration / count : 0,
                "avgCalories", count > 0 ? totalCalories / count : 0
        );
    }

    static ActivityType typeOf(ActivityView a) {
        return a.activityType() != null ? ActivityType.valueOf(a.activityType()) : ActivityType.OTHER;
    }
}
