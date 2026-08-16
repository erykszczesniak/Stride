package com.strava.webbff.report;

import com.strava.webbff.client.ActivityView;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * POST /api/v1/export/report/csv — ported 1:1 from the monolith's
 * {@code ExportController#exportReportCsv}: same request body
 * ({dateFrom, dateTo, fields, activityType, units}), same header row (the raw
 * field ids), same per-field value derivations (including the "0.0" that the
 * monolith's int-vs-Double ternary promotion produced for missing distances),
 * same octet-stream + form-data Content-Disposition.
 *
 * <p>The monolith's other export endpoints (activities json/csv/txt/gpx) moved
 * to activity-service with the rest of the activity domain — the gateway
 * routing keeps the whole /api/v1/export/** surface on this service only for
 * the report CSV, matching what the frontend actually calls.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/export")
public class ExportController {

    private static final DateTimeFormatter ROW_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ReportService reportService;

    @PostMapping("/report/csv")
    public ResponseEntity<byte[]> exportReportCsv(@RequestBody Map<String, Object> request) {
        String dateFrom = (String) request.get("dateFrom");
        String dateTo = (String) request.get("dateTo");
        String activityTypeStr = (String) request.get("activityType");
        boolean imperial = "imperial".equals(request.get("units"));
        @SuppressWarnings("unchecked")
        List<String> fields = (List<String>) request.get("fields");

        List<ActivityView> activities = reportService.activitiesFor(dateFrom, dateTo, activityTypeStr);

        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", fields)).append("\n");

        for (ActivityView a : activities) {
            List<String> values = new java.util.ArrayList<>();
            for (String field : fields) {
                values.add(switch (field) {
                    case "activityTitle" -> quote(a.title());
                    case "activityType" -> a.activityType();
                    case "date" -> a.startTime() != null
                            ? a.startTime().format(ROW_DATE_FORMAT) : "";
                    case "distance" -> String.valueOf(a.distance() != null ? a.distance() : 0);
                    case "duration" -> formatDuration(a.duration() != null ? a.duration() : 0);
                    case "elevationGain" -> String.valueOf(a.elevationGain() != null ? a.elevationGain() : 0);
                    case "calories" -> String.valueOf(a.calories() != null ? a.calories() : 0);
                    case "averageSpeed" -> String.valueOf(a.averageSpeed() != null ? a.averageSpeed() : 0);
                    case "averageHeartRate" -> String.valueOf(a.averageHeartRate() != null ? a.averageHeartRate() : 0);
                    case "pace" -> PaceFormatter.format(ReportService.typeOf(a), a.distance(), a.duration(), imperial);
                    case "likesCount" -> String.valueOf(a.likesCount());
                    case "commentsCount" -> String.valueOf(a.commentsCount());
                    default -> "";
                });
            }
            csv.append(String.join(",", values)).append("\n");
        }

        byte[] csvBytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "report.csv");
        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
    }

    /** Ported 1:1 from main's ExportController#formatDuration. */
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

    /** Ported 1:1 from main's ExportController#quote. */
    private String quote(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
