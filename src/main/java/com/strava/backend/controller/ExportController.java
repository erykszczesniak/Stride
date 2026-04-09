package com.strava.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.backend.domain.Activity;
import com.strava.backend.dto.activity.ActivityResponse;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.mapper.CommonMapper;
import com.strava.backend.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static com.strava.backend.constants.ErrorMessage.ACTIVITY_NOT_FOUND;
import static com.strava.backend.constants.ErrorMessage.ACTIVITY_ACCESS_DENIED;
import static com.strava.backend.constants.PathConstants.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(API_V1_EXPORT)
public class ExportController extends BaseController {

    private final ActivityRepository activityRepository;
    private final CommonMapper commonMapper;
    private final ObjectMapper objectMapper;


    @GetMapping(ACTIVITIES + JSON)
    public ResponseEntity<byte[]> exportActivitiesAsJson() throws Exception {
        Long currentUserId = getAuthenticatedUserId();
        List<Activity> activities = activityRepository.findByUserIdOrderByCreatedAtDesc(currentUserId);
        List<ActivityResponse> responses = commonMapper.convertToResponseList(activities, ActivityResponse.class);

        byte[] jsonBytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(responses);

        String filename = "activities_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".json";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonBytes);
    }

    @GetMapping(ACTIVITIES + CSV)
    public ResponseEntity<byte[]> exportActivitiesAsCsv() {
        Long currentUserId = getAuthenticatedUserId();
        List<Activity> activities = activityRepository.findByUserIdOrderByCreatedAtDesc(currentUserId);

        StringBuilder csv = new StringBuilder();
        csv.append("ID,Title,Activity Type,Distance (m),Duration (s),Calories,Elevation Gain,Start Time,Description\n");

        for (Activity activity : activities) {
            csv.append(escapeCSV(activity.getId().toString())).append(",");
            csv.append(escapeCSV(activity.getTitle())).append(",");
            csv.append(escapeCSV(activity.getActivityType().name())).append(",");
            csv.append(activity.getDistance() != null ? activity.getDistance() : "").append(",");
            csv.append(activity.getDuration() != null ? activity.getDuration() : "").append(",");
            csv.append(activity.getCalories() != null ? activity.getCalories() : "").append(",");
            csv.append(activity.getElevationGain() != null ? activity.getElevationGain() : "").append(",");
            csv.append(activity.getStartTime() != null ? activity.getStartTime().toString() : "").append(",");
            csv.append(escapeCSV(activity.getDescription() != null ? activity.getDescription() : ""));
            csv.append("\n");
        }

        String filename = "activities_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString().getBytes());
    }

    @GetMapping(ACTIVITIES + TXT)
    public ResponseEntity<byte[]> exportActivitiesAsTxt() {
        Long currentUserId = getAuthenticatedUserId();
        List<Activity> activities = activityRepository.findByUserIdOrderByCreatedAtDesc(currentUserId);

        StringBuilder txt = new StringBuilder();
        txt.append("=".repeat(60)).append("\n");
        txt.append("           ACTIVITIES EXPORT REPORT\n");
        txt.append("           Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        txt.append("=".repeat(60)).append("\n\n");

        txt.append("Total activities: ").append(activities.size()).append("\n\n");

        for (int i = 0; i < activities.size(); i++) {
            Activity activity = activities.get(i);
            txt.append("-".repeat(60)).append("\n");
            txt.append("Activity #").append(i + 1).append("\n");
            txt.append("-".repeat(60)).append("\n");
            txt.append("Title:          ").append(activity.getTitle()).append("\n");
            txt.append("Type:           ").append(activity.getActivityType().name()).append("\n");
            txt.append("Date:           ").append(activity.getStartTime() != null ?
                    activity.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "N/A").append("\n");
            txt.append("Distance:       ").append(formatDistance(activity.getDistance())).append("\n");
            txt.append("Duration:       ").append(formatDuration(activity.getDuration())).append("\n");
            txt.append("Avg Speed:      ").append(activity.getAverageSpeed() != null ?
                    String.format("%.2f km/h", activity.getAverageSpeed()) : "N/A").append("\n");
            txt.append("Elevation Gain: ").append(activity.getElevationGain() != null ?
                    activity.getElevationGain() + " m" : "N/A").append("\n");
            txt.append("Calories:       ").append(activity.getCalories() != null ?
                    activity.getCalories() + " kcal" : "N/A").append("\n");
            if (activity.getDescription() != null && !activity.getDescription().isEmpty()) {
                txt.append("Description:    ").append(activity.getDescription()).append("\n");
            }
            txt.append("\n");
        }

        txt.append("=".repeat(60)).append("\n");
        txt.append("                 END OF REPORT\n");
        txt.append("=".repeat(60)).append("\n");

        String filename = "activities_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(txt.toString().getBytes());
    }

    @GetMapping(ACTIVITIES + GPX + ACTIVITY_ID)
    public ResponseEntity<byte[]> exportActivityAsGpx(@PathVariable Long activityId) {
        Long currentUserId = getAuthenticatedUserId();
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (!activity.getUser().getId().equals(currentUserId)) {
            throw new ApiRequestException(ACTIVITY_ACCESS_DENIED, HttpStatus.FORBIDDEN);
        }

        StringBuilder gpx = new StringBuilder();
        gpx.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        gpx.append("<gpx version=\"1.1\" creator=\"StravaClone\">\n");
        gpx.append("  <metadata>\n");
        gpx.append("    <name>").append(escapeXML(activity.getTitle())).append("</name>\n");
        gpx.append("    <time>").append(activity.getStartTime() != null ? activity.getStartTime().toString() : "").append("</time>\n");
        gpx.append("  </metadata>\n");
        gpx.append("  <trk>\n");
        gpx.append("    <name>").append(escapeXML(activity.getTitle())).append("</name>\n");
        gpx.append("    <type>").append(activity.getActivityType().name()).append("</type>\n");

        if (activity.getRoutePolyline() != null && !activity.getRoutePolyline().isEmpty()) {
            gpx.append("    <trkseg>\n");
            gpx.append("      <!-- Route polyline: ").append(activity.getRoutePolyline()).append(" -->\n");
            gpx.append("    </trkseg>\n");
        }

        gpx.append("  </trk>\n");
        gpx.append("</gpx>");

        String filename = "activity_" + activityId + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".gpx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_XML)
                .body(gpx.toString().getBytes());
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String escapeXML(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String formatDistance(Double distance) {
        if (distance == null) return "N/A";
        if (distance >= 1) {
            return String.format("%.2f km", distance);
        } else {
            return String.format("%.0f m", distance * 1000);
        }
    }

    private String formatDuration(Integer durationSeconds) {
        if (durationSeconds == null) return "N/A";
        int hours = durationSeconds / 3600;
        int minutes = (durationSeconds % 3600) / 60;
        int seconds = durationSeconds % 60;
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    @PostMapping("/report/csv")
    public ResponseEntity<byte[]> exportReportCsv(@RequestBody Map<String, Object> request) {
        Long currentUserId = getAuthenticatedUserId();
        String dateFrom = (String) request.get("dateFrom");
        String dateTo = (String) request.get("dateTo");
        String activityTypeStr = (String) request.get("activityType");
        @SuppressWarnings("unchecked")
        List<String> fields = (List<String>) request.get("fields");

        LocalDateTime from = LocalDateTime.parse(dateFrom + "T00:00:00");
        LocalDateTime to = LocalDateTime.parse(dateTo + "T23:59:59");

        List<Activity> activities;
        if (activityTypeStr != null && !activityTypeStr.isEmpty()) {
            ActivityType type = ActivityType.valueOf(activityTypeStr);
            activities = activityRepository.findByUserIdAndActivityTypeAndDateRange(currentUserId, type, from, to);
        } else {
            activities = activityRepository.findByUserIdAndDateRange(currentUserId, from, to);
        }

        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", fields)).append("\n");

        for (Activity a : activities) {
            List<String> values = new java.util.ArrayList<>();
            for (String field : fields) {
                values.add(switch (field) {
                    case "activityTitle" -> quote(a.getTitle());
                    case "activityType" -> a.getActivityType().name();
                    case "date" -> a.getStartTime() != null
                            ? a.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "";
                    case "distance" -> String.valueOf(a.getDistance() != null ? a.getDistance() : 0);
                    case "duration" -> formatDuration(a.getDuration() != null ? a.getDuration() : 0);
                    case "elevationGain" -> String.valueOf(a.getElevationGain() != null ? a.getElevationGain() : 0);
                    case "calories" -> String.valueOf(a.getCalories() != null ? a.getCalories() : 0);
                    case "averageSpeed" -> String.valueOf(a.getAverageSpeed() != null ? a.getAverageSpeed() : 0);
                    case "averageHeartRate" -> String.valueOf(a.getAverageHeartRate() != null ? a.getAverageHeartRate() : 0);
                    case "pace" -> calculatePace(a);
                    case "likesCount" -> String.valueOf(a.getLikesCount());
                    case "commentsCount" -> String.valueOf(a.getComments() != null ? a.getComments().size() : 0);
                    default -> "";
                });
            }
            csv.append(String.join(",", values)).append("\n");
        }

        byte[] csvBytes = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "report.csv");
        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
    }

    private String quote(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private String calculatePace(Activity a) {
        if (a.getDistance() == null || a.getDistance() == 0 || a.getDuration() == null || a.getDuration() == 0) return "-";
        double pace = (a.getDuration() / 60.0) / a.getDistance();
        int mins = (int) pace;
        int secs = (int) ((pace - mins) * 60);
        return String.format("%d:%02d", mins, secs);
    }
}
