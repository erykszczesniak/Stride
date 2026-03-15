package com.strava.backend.service.impl;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.Event;
import com.strava.backend.domain.Goal;
import com.strava.backend.domain.User;
import com.strava.backend.dto.DashboardResponse;
import com.strava.backend.dto.activity.ActivityResponse;
import com.strava.backend.dto.activity.ActivityStatsResponse;
import com.strava.backend.dto.activity.WeeklyStatsResponse;
import com.strava.backend.dto.event.EventResponse;
import com.strava.backend.dto.goal.GoalResponse;
import com.strava.backend.dto.user.UserResponse;
import com.strava.backend.enums.GoalPeriod;
import com.strava.backend.mapper.CommonMapper;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.repository.UserRepository;
import com.strava.backend.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserService userService;
    private final ActivityService activityService;
    private final GoalService goalService;
    private final EventService eventService;
    private final CommonMapper commonMapper;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboardData(Long userId) {
        User user = userService.getUserById(userId);

        // Get user response with counts
        UserResponse userResponse = mapToUserResponse(user);

        // Get activity stats
        ActivityStatsResponse stats = activityService.getMonthlyStats(userId);

        // Get weekly stats
        WeeklyStatsResponse weeklyStats = activityService.getWeeklyStats(userId);

        // Get recent activities
        List<Activity> recentActivities = activityService.getRecentActivitiesByUserId(userId, 5);
        List<ActivityResponse> activityResponses = recentActivities.stream()
                .map(activity -> mapToActivityResponse(activity, userId))
                .collect(Collectors.toList());

        // Get weekly goal
        GoalResponse weeklyGoal = null;
        Optional<Goal> activeWeeklyGoal = goalService.getActiveGoalByPeriod(userId, GoalPeriod.WEEKLY);
        if (activeWeeklyGoal.isPresent()) {
            weeklyGoal = mapToGoalResponse(activeWeeklyGoal.get());
        }

        // Get upcoming events
        List<Event> upcomingEvents = eventService.getUpcomingEventsByUserId(userId, 3);
        List<EventResponse> eventResponses = upcomingEvents.stream()
                .map(event -> mapToEventResponse(event, userId))
                .collect(Collectors.toList());

        return DashboardResponse.builder()
                .user(userResponse)
                .stats(stats)
                .weeklyStats(weeklyStats)
                .recentActivities(activityResponses)
                .weeklyGoal(weeklyGoal)
                .upcomingEvents(eventResponses)
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        UserResponse response = commonMapper.convertToResponse(user, UserResponse.class);
        response.setFollowersCount(userRepository.countFollowers(user.getId()));
        response.setFollowingCount(userRepository.countFollowing(user.getId()));
        response.setActivitiesCount(activityRepository.countByUserId(user.getId()));
        return response;
    }

    private ActivityResponse mapToActivityResponse(Activity activity, Long currentUserId) {
        ActivityResponse response = commonMapper.convertToResponse(activity, ActivityResponse.class);
        response.setLikesCount(activity.getLikesCount());
        response.setCommentsCount(activity.getCommentsCount());
        response.setLikedByCurrentUser(activityService.isLikedByUser(activity.getId(), currentUserId));
        return response;
    }

    private GoalResponse mapToGoalResponse(Goal goal) {
        GoalResponse response = commonMapper.convertToResponse(goal, GoalResponse.class);
        response.setProgressPercentage(goal.getProgressPercentage());
        response.setRemainingValue(goal.getRemainingValue());
        response.setCompleted(goal.isCompleted());
        return response;
    }

    private EventResponse mapToEventResponse(Event event, Long currentUserId) {
        EventResponse response = commonMapper.convertToResponse(event, EventResponse.class);
        response.setRegisteredCount(event.getRegisteredCount());
        response.setRegistered(eventService.isUserRegistered(event.getId(), currentUserId));
        return response;
    }
}
