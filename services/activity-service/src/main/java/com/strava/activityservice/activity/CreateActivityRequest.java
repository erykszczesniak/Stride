package com.strava.activityservice.activity;

import com.strava.activityservice.domain.ActivityType;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record CreateActivityRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 2000) String description,
        @NotNull ActivityType activityType,
        @NotNull @Positive Double distance,         // km
        @NotNull @Positive Integer duration,        // seconds
        Integer elevationGain,                       // meters
        Integer calories,
        Double averageSpeed,
        Double maxSpeed,
        Integer averageHeartRate,
        Integer maxHeartRate,
        @NotNull LocalDateTime startTime,
        LocalDateTime endTime,
        String startLocation,
        String endLocation,
        String routePolyline,
        String mapImageUrl,
        Boolean isPrivate
) {}
