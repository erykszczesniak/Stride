package com.strava.backend.dto.activity;

import com.strava.backend.enums.ActivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

import static com.strava.backend.constants.ErrorMessage.*;

@Data
public class ActivityRequest {

    @NotBlank(message = EMPTY_ACTIVITY_TITLE)
    private String title;

    private String description;

    @NotNull(message = "Activity type is required")
    private ActivityType activityType;

    @NotNull(message = INVALID_DISTANCE)
    @Positive(message = INVALID_DISTANCE)
    private Double distance;

    @NotNull(message = INVALID_DURATION)
    @Positive(message = INVALID_DURATION)
    private Integer duration;

    private Integer elevationGain;
    private Integer calories;
    private Double averageSpeed;
    private Double maxSpeed;
    private Integer averageHeartRate;
    private Integer maxHeartRate;
    private Integer averageCadence;
    private Integer averagePower;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    private String startLocation;
    private String endLocation;
    private String routePolyline;
    private boolean isPrivate;
}
