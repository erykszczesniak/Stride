package com.strava.webbff.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.strava.webbff.client.ActivityClient;
import com.strava.webbff.client.EventClient;
import com.strava.webbff.client.GoalClient;
import com.strava.webbff.client.UserClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Composes the dashboard exactly like the monolith's {@code DashboardServiceImpl}:
 * <ul>
 *   <li>{@code user} — user-service {@code /users/me} (UserResponse incl. counts);</li>
 *   <li>{@code stats} — activity-service monthly stats;</li>
 *   <li>{@code weeklyStats} — activity-service weekly stats;</li>
 *   <li>{@code recentActivities} — last {@value #RECENT_ACTIVITIES_LIMIT}, newest
 *       first, social fields enriched by activity-service;</li>
 *   <li>{@code weeklyGoal} — the active WEEKLY goal or null (main:
 *       {@code getActiveGoalByPeriod(userId, WEEKLY)});</li>
 *   <li>{@code upcomingEvents} — next {@value #UPCOMING_EVENTS_LIMIT} events the
 *       user registered for, soonest first (main:
 *       {@code getUpcomingEventsByUserId(userId, 3)}).</li>
 * </ul>
 * Every section degrades independently (null / empty list) when its owning
 * service is unavailable — the clients' Resilience4j fallbacks guarantee this
 * method never throws because of a downstream outage.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    static final int RECENT_ACTIVITIES_LIMIT = 5;
    static final int UPCOMING_EVENTS_LIMIT = 3;

    private final UserClient userClient;
    private final ActivityClient activityClient;
    private final GoalClient goalClient;
    private final EventClient eventClient;

    public DashboardResponse getDashboardData() {
        JsonNode user = userClient.currentUser();
        JsonNode stats = activityClient.monthlyStats();
        JsonNode weeklyStats = activityClient.weeklyStats();
        List<JsonNode> recentActivities = activityClient.recentActivities(RECENT_ACTIVITIES_LIMIT);

        JsonNode weeklyGoal = goalClient.activeGoals().stream()
                .filter(goal -> "WEEKLY".equals(goal.path("period").asText()))
                .findFirst()
                .orElse(null);

        List<JsonNode> upcomingEvents = eventClient.myUpcomingEvents().stream()
                .limit(UPCOMING_EVENTS_LIMIT)
                .toList();

        return new DashboardResponse(user, stats, weeklyStats, recentActivities, weeklyGoal, upcomingEvents);
    }
}
