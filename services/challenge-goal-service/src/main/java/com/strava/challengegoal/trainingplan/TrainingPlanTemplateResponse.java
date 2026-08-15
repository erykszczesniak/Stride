package com.strava.challengegoal.trainingplan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Verbatim copy of the monolith's TrainingPlanTemplateResponse. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingPlanTemplateResponse {
    private String id;
    private String title;
    private String description;
    private String level;
    private String levelColor;
    private String duration;
    private String frequency;
    private String goal;
    private String gradient;
    private String icon;
    private List<WeekTemplate> weeks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeekTemplate {
        private int week;
        private String name;
        private String description;
        private List<WorkoutTemplate> workouts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkoutTemplate {
        private String day;
        private String type;
        private String description;
        private String duration;
    }
}
