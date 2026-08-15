package com.strava.activityservice.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Ported 1:1 from main. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyStatsResponse {
    private Double totalDistance;
    private String trendPercentage;
    private List<DailyStats> dailyStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyStats {
        private String dayName;
        private Double distance;
        private Integer duration;
        private Integer activities;
    }
}
