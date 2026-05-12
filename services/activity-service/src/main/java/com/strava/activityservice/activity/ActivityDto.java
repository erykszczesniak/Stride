package com.strava.activityservice.activity;

import com.strava.activityservice.domain.Activity;
import com.strava.activityservice.domain.ActivityType;

import java.time.LocalDateTime;

public record ActivityDto(
        Long id,
        Long userId,
        String title,
        String description,
        ActivityType activityType,
        Double distance,
        Integer duration,
        Integer elevationGain,
        Integer calories,
        Double averageSpeed,
        Double maxSpeed,
        Integer averageHeartRate,
        Integer maxHeartRate,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String startLocation,
        String endLocation,
        String routePolyline,
        String mapImageUrl,
        boolean isPrivate,
        LocalDateTime createdAt
) {
    public static ActivityDto from(Activity a) {
        return new ActivityDto(
                a.getId(), a.getUserId(), a.getTitle(), a.getDescription(), a.getActivityType(),
                a.getDistance(), a.getDuration(), a.getElevationGain(), a.getCalories(),
                a.getAverageSpeed(), a.getMaxSpeed(), a.getAverageHeartRate(), a.getMaxHeartRate(),
                a.getStartTime(), a.getEndTime(),
                a.getStartLocation(), a.getEndLocation(),
                a.getRoutePolyline(), a.getMapImageUrl(),
                a.isPrivate(), a.getCreatedAt()
        );
    }
}
