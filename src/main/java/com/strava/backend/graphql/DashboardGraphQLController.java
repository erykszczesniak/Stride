package com.strava.backend.graphql;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.Event;
import com.strava.backend.domain.Goal;
import com.strava.backend.domain.User;
import com.strava.backend.dto.activity.ActivityStatsResponse;
import com.strava.backend.dto.activity.WeeklyStatsResponse;
import com.strava.backend.enums.GoalPeriod;
import com.strava.backend.security.UserPrincipal;
import com.strava.backend.service.ActivityService;
import com.strava.backend.service.EventService;
import com.strava.backend.service.GoalService;
import com.strava.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class DashboardGraphQLController {

    private final UserService userService;
    private final ActivityService activityService;
    private final GoalService goalService;
    private final EventService eventService;

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) authentication.getPrincipal()).getId();
        }
        return null;
    }

    @QueryMapping
    public DashboardData dashboard() {
        return new DashboardData(getAuthenticatedUserId());
    }

    // Inner class to hold dashboard context
    public record DashboardData(Long userId) {}

    @SchemaMapping(typeName = "Dashboard", field = "user")
    public User user(DashboardData dashboard) {
        return userService.getUserById(dashboard.userId());
    }

    @SchemaMapping(typeName = "Dashboard", field = "stats")
    public ActivityStatsResponse stats(DashboardData dashboard) {
        return activityService.getMonthlyStats(dashboard.userId());
    }

    @SchemaMapping(typeName = "Dashboard", field = "weeklyStats")
    public WeeklyStatsResponse weeklyStats(DashboardData dashboard) {
        return activityService.getWeeklyStats(dashboard.userId());
    }

    @SchemaMapping(typeName = "Dashboard", field = "recentActivities")
    public List<Activity> recentActivities(DashboardData dashboard) {
        return activityService.getRecentActivitiesByUserId(dashboard.userId(), 5);
    }

    @SchemaMapping(typeName = "Dashboard", field = "weeklyGoal")
    public Goal weeklyGoal(DashboardData dashboard) {
        return goalService.getActiveGoalByPeriod(dashboard.userId(), GoalPeriod.WEEKLY).orElse(null);
    }

    @SchemaMapping(typeName = "Dashboard", field = "upcomingEvents")
    public List<Event> upcomingEvents(DashboardData dashboard) {
        return eventService.getUpcomingEventsByUserId(dashboard.userId(), 5);
    }
}
