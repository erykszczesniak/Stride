package com.strava.activityservice.internal;

import com.strava.activityservice.domain.Activity;
import com.strava.activityservice.domain.ActivityType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cluster-internal batch payload: every ActivityResponse field except the
 * social counts and the embedded user object, plus userId and the full
 * detail-level metrics. This shape is a cross-service contract (social feed
 * hydration, AI context, BFF reports) — do not change field names.
 */
public record InternalActivityResponse(
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
        Integer averageCadence,
        Integer averagePower,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String startLocation,
        String endLocation,
        String routePolyline,
        double[] elevationProfile,
        long[] pointTimestamps,
        String mapImageUrl,
        boolean isPrivate,
        List<String> photos,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static InternalActivityResponse from(Activity a, double[] elevationProfile, long[] pointTimestamps) {
        return new InternalActivityResponse(
                a.getId(), a.getUserId(), a.getTitle(), a.getDescription(), a.getActivityType(),
                a.getDistance(), a.getDuration(), a.getElevationGain(), a.getCalories(),
                a.getAverageSpeed(), a.getMaxSpeed(), a.getAverageHeartRate(), a.getMaxHeartRate(),
                a.getAverageCadence(), a.getAveragePower(),
                a.getStartTime(), a.getEndTime(), a.getStartLocation(), a.getEndLocation(),
                a.getRoutePolyline(), elevationProfile, pointTimestamps,
                a.getMapImageUrl(), a.isPrivate(),
                // Detached copy: the record outlives the transaction that loaded
                // the entity (open-in-view is off), so the live lazy collection
                // must be materialized here, not during JSON serialization.
                a.getPhotos() != null ? List.copyOf(a.getPhotos()) : List.of(),
                a.getCreatedAt(), a.getUpdatedAt()
        );
    }
}
