package com.strava.backend.graphql;

import com.strava.backend.domain.Activity;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.controller.BaseController;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequiredArgsConstructor
public class ReportGraphQLController extends BaseController {

    private final ActivityRepository activityRepository;

    @QueryMapping
    public Map<String, Object> generateReport(
            @Argument String dateFrom,
            @Argument String dateTo,
            @Argument List<String> fields,
            @Argument String activityType) {

        Long userId = getAuthenticatedUserId();
        LocalDateTime from = LocalDateTime.parse(dateFrom + "T00:00:00");
        LocalDateTime to = LocalDateTime.parse(dateTo + "T23:59:59");

        List<Activity> activities;
        if (activityType != null && !activityType.isEmpty()) {
            ActivityType type = ActivityType.valueOf(activityType);
            activities = activityRepository.findByUserIdAndActivityTypeAndDateRange(userId, type, from, to);
        } else {
            activities = activityRepository.findByUserIdAndDateRange(userId, from, to);
        }

        List<Map<String, Object>> rows = activities.stream()
                .map(a -> buildRow(a, fields))
                .toList();

        Map<String, Object> summary = buildSummary(activities);

        return Map.of("rows", rows, "summary", summary);
    }

    private Map<String, Object> buildRow(Activity a, List<String> fields) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (String field : fields) {
            switch (field) {
                case "activityTitle" -> row.put("activityTitle", a.getTitle());
                case "activityType" -> row.put("activityType", a.getActivityType().name());
                case "date" -> row.put("date", a.getStartTime() != null
                        ? a.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                        : a.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                case "distance" -> row.put("distance", a.getDistance());
                case "duration" -> row.put("duration", a.getDuration());
                case "elevationGain" -> row.put("elevationGain", a.getElevationGain());
                case "calories" -> row.put("calories", a.getCalories());
                case "averageSpeed" -> row.put("averageSpeed", a.getAverageSpeed());
                case "averageHeartRate" -> row.put("averageHeartRate", a.getAverageHeartRate());
                case "pace" -> row.put("pace", calculatePace(a));
                case "likesCount" -> row.put("likesCount", a.getLikesCount());
                case "commentsCount" -> row.put("commentsCount", a.getComments() != null ? a.getComments().size() : 0);
                default -> {}
            }
        }
        return row;
    }

    private String calculatePace(Activity a) {
        if (a.getDistance() == null || a.getDistance() == 0 || a.getDuration() == null || a.getDuration() == 0) {
            return "-";
        }
        double paceMinPerKm = (a.getDuration() / 60.0) / a.getDistance();
        int mins = (int) paceMinPerKm;
        int secs = (int) ((paceMinPerKm - mins) * 60);
        return String.format("%d:%02d min/km", mins, secs);
    }

    private Map<String, Object> buildSummary(List<Activity> activities) {
        double totalDistance = activities.stream().mapToDouble(a -> a.getDistance() != null ? a.getDistance() : 0).sum();
        int totalDuration = activities.stream().mapToInt(a -> a.getDuration() != null ? a.getDuration() : 0).sum();
        int totalCalories = activities.stream().mapToInt(a -> a.getCalories() != null ? a.getCalories() : 0).sum();
        int totalElevation = activities.stream().mapToInt(a -> a.getElevationGain() != null ? a.getElevationGain() : 0).sum();
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
}
