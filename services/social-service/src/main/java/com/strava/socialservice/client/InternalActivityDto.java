package com.strava.socialservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Client-side mirror of activity-service's InternalActivityResponse (the
 * cluster-internal batch payload). Component names match the producer's
 * record 1:1 so Jackson round-trips symmetrically; unknown fields are
 * ignored so activity-service can add fields without breaking us.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InternalActivityDto(
        Long id,
        Long userId,
        String title,
        String description,
        String activityType,
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
) {}
