package com.strava.backend.dto.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
