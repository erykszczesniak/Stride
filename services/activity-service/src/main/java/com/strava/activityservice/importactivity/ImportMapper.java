package com.strava.activityservice.importactivity;

import com.strava.activityservice.activity.ActivityMapper;
import com.strava.activityservice.activity.ActivityResponse;
import com.strava.activityservice.domain.Activity;
import com.strava.activityservice.domain.ActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Response assembly for the import flow, ported from main's ImportMapper.
 * Delegates activity mapping to ActivityMapper so the nested activity JSON is
 * byte-identical to what POST /api/v1/activities returns.
 */
@Component
@RequiredArgsConstructor
public class ImportMapper {

    private final ImportService importService;
    private final ActivityMapper activityMapper;

    public ImportResponse importActivity(Long userId, MultipartFile file, String titleOverride,
                                         ActivityType activityTypeOverride, boolean isPrivate) {
        Activity activity = importService.importActivity(userId, file, titleOverride, activityTypeOverride, isPrivate);
        ActivityResponse activityResponse = activityMapper.mapToActivityResponse(activity, userId);

        String filename = file.getOriginalFilename();
        String extension = filename != null && filename.contains(".")
                ? filename.substring(filename.lastIndexOf('.') + 1).toUpperCase()
                : "UNKNOWN";

        int gpsPoints = activity.getRoutePolyline() != null && !activity.getRoutePolyline().isEmpty()
                ? estimatePointCount(activity.getRoutePolyline())
                : 0;

        return ImportResponse.builder()
                .activity(activityResponse)
                .sourceFormat(extension)
                .sourceFilename(filename)
                .gpsPointsImported(gpsPoints)
                .build();
    }

    private int estimatePointCount(String polyline) {
        // Rough estimate: each point pair takes ~8-12 encoded chars on average
        return Math.max(1, polyline.length() / 10);
    }
}
