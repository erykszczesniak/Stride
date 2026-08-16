package com.strava.activityservice.activity;

import com.strava.activityservice.domain.ActivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

import static com.strava.activityservice.constants.ErrorMessage.*;

/**
 * Ported 1:1 from main (field names, validation messages and the Lombok
 * boolean quirk: {@code isPrivate} binds to the JSON property "private").
 */
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

    /**
     * JSON-serialized array of epoch-seconds longs, one per polyline point.
     * Populated by importers that have per-trackpoint time data (GPX/TCX/FIT, Strava streams).
     * Null for manually-logged activities; consumers fall back to uniform-pace splits.
     */
    private String pointTimestamps;

    /**
     * JSON-serialized array of doubles (meters), one per polyline point.
     * Populated by importers that have per-trackpoint altitude (GPX/TCX/FIT, Strava streams).
     * Null when no altitude data is available.
     */
    private String elevationProfile;

    private boolean isPrivate;

    /**
     * Optional gear to tag this activity with. Must belong to the user and
     * support the activity's type — service layer enforces both.
     */
    private Long gearId;
}
