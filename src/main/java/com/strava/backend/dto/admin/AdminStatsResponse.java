package com.strava.backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponse {
    private long totalUsers;
    private long activeUsers;
    private long totalActivities;
    private long totalClubs;
    private long totalChallenges;
    private long totalEvents;
    private long newUsersThisWeek;
    private long activitiesThisWeek;
}
