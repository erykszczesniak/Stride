package com.strava.backend.dto;

import com.strava.backend.dto.activity.ActivityResponse;
import com.strava.backend.dto.activity.ActivityStatsResponse;
import com.strava.backend.dto.activity.WeeklyStatsResponse;
import com.strava.backend.dto.event.EventResponse;
import com.strava.backend.dto.goal.GoalResponse;
import com.strava.backend.dto.user.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private UserResponse user;
    private ActivityStatsResponse stats;
    private WeeklyStatsResponse weeklyStats;
    private List<ActivityResponse> recentActivities;
    private GoalResponse weeklyGoal;
    private List<EventResponse> upcomingEvents;
}
