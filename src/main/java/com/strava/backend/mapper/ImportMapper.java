package com.strava.backend.mapper;

import com.strava.backend.domain.Activity;
import com.strava.backend.dto.activity.ActivityResponse;
import com.strava.backend.dto.importactivity.ImportResponse;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.service.ActivityService;
import com.strava.backend.service.ImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class ImportMapper {

    private final CommonMapper commonMapper;
    private final ImportService importService;
    private final ActivityService activityService;

    public ImportResponse importActivity(Long userId, MultipartFile file, String titleOverride,
                                         ActivityType activityTypeOverride, boolean isPrivate) {
        Activity activity = importService.importActivity(userId, file, titleOverride, activityTypeOverride, isPrivate);
        ActivityResponse activityResponse = mapToActivityResponse(activity, userId);

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

    private ActivityResponse mapToActivityResponse(Activity activity, Long currentUserId) {
        ActivityResponse response = commonMapper.convertToResponse(activity, ActivityResponse.class);
        response.setLikesCount(activity.getLikesCount());
        response.setCommentsCount(activity.getCommentsCount());
        response.setLikedByCurrentUser(activityService.isLikedByUser(activity.getId(), currentUserId));
        return response;
    }

    private int estimatePointCount(String polyline) {
        // Rough estimate: each point pair takes ~8-12 encoded chars on average
        return Math.max(1, polyline.length() / 10);
    }
}
