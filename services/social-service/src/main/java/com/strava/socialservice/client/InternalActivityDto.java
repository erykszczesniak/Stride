package com.strava.socialservice.client;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Client-side mirror of activity-service's InternalActivityResponse (the
 * cluster-internal batch payload). Component names still match the producer's
 * record 1:1; since the gRPC cutover the values are mapped from the
 * ActivityPayload wire message ({@link ActivityBatchClient#activitiesById}),
 * with proto3 optional fields translated back to nulls so feed hydration
 * sees exactly the shape the REST JSON used to deliver.
 */
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
