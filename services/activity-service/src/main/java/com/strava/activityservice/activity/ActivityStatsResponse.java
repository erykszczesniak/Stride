package com.strava.activityservice.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Ported 1:1 from main. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityStatsResponse {
    private Double totalDistance;
    private Long totalDuration; // in seconds
    private Integer totalCalories;
    private Integer totalActivities;
    private Integer totalElevationGain;
    private Double averageDistance;
    private Double averageDuration;
    private String trendPercentage;
    private Double distanceTrendPercentage;
    private Double durationTrendPercentage;
    private Double caloriesTrendPercentage;
    private Double activitiesTrendPercentage;
}
