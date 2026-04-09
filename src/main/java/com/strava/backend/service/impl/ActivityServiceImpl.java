package com.strava.backend.service.impl;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.User;
import com.strava.backend.dto.activity.ActivityRequest;
import com.strava.backend.dto.activity.ActivityStatsResponse;
import com.strava.backend.dto.activity.WeeklyStatsResponse;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.enums.NotificationType;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.service.ActivityService;
import com.strava.backend.service.NotificationService;
import com.strava.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.strava.backend.constants.ErrorMessage.ACTIVITY_ACCESS_DENIED;
import static com.strava.backend.constants.ErrorMessage.ACTIVITY_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public Activity getActivityById(Long activityId) {
        return activityRepository.findById(activityId)
                .orElseThrow(() -> new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Activity> getActivitiesByUserId(Long userId, Pageable pageable) {
        return activityRepository.findByUserId(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Activity> getRecentActivitiesByUserId(Long userId, int limit) {
        return activityRepository.findByUserId(userId, PageRequest.of(0, limit))
                .getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Activity> getFeedActivities(Long userId, Pageable pageable) {
        User user = userService.getUserById(userId);
        List<Long> followingIds = user.getFollowing().stream()
                .map(User::getId)
                .collect(Collectors.toList());
        followingIds.add(userId);
        return activityRepository.findByUserIdIn(followingIds, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Activity> getPublicActivities(Pageable pageable) {
        return activityRepository.findByIsPrivateFalse(pageable);
    }

    @Override
    @Transactional
    public Activity createActivity(Long userId, ActivityRequest request) {
        User user = userService.getUserById(userId);

        Activity activity = Activity.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .activityType(request.getActivityType())
                .distance(request.getDistance())
                .duration(request.getDuration())
                .elevationGain(request.getElevationGain())
                .calories(request.getCalories())
                .averageSpeed(request.getAverageSpeed())
                .maxSpeed(request.getMaxSpeed())
                .averageHeartRate(request.getAverageHeartRate())
                .maxHeartRate(request.getMaxHeartRate())
                .averageCadence(request.getAverageCadence())
                .averagePower(request.getAveragePower())
                .startTime(request.getStartTime())
                .startLocation(request.getStartLocation())
                .endLocation(request.getEndLocation())
                .routePolyline(request.getRoutePolyline())
                .isPrivate(request.isPrivate())
                .user(user)
                .build();

        return activityRepository.save(activity);
    }

    @Override
    @Transactional
    public Activity updateActivity(Long activityId, Long userId, ActivityRequest request) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (!activity.getUser().getId().equals(userId)) {
            throw new ApiRequestException(ACTIVITY_ACCESS_DENIED, HttpStatus.FORBIDDEN);
        }

        activity.setTitle(request.getTitle());
        activity.setDescription(request.getDescription());
        activity.setActivityType(request.getActivityType());
        activity.setDistance(request.getDistance());
        activity.setDuration(request.getDuration());
        activity.setElevationGain(request.getElevationGain());
        activity.setCalories(request.getCalories());
        activity.setAverageSpeed(request.getAverageSpeed());
        activity.setMaxSpeed(request.getMaxSpeed());
        activity.setAverageHeartRate(request.getAverageHeartRate());
        activity.setMaxHeartRate(request.getMaxHeartRate());
        activity.setAverageCadence(request.getAverageCadence());
        activity.setAveragePower(request.getAveragePower());
        activity.setStartTime(request.getStartTime());
        activity.setStartLocation(request.getStartLocation());
        activity.setEndLocation(request.getEndLocation());
        activity.setRoutePolyline(request.getRoutePolyline());
        activity.setPrivate(request.isPrivate());

        return activityRepository.save(activity);
    }

    @Override
    @Transactional
    public void deleteActivity(Long activityId, Long userId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (!activity.getUser().getId().equals(userId)) {
            throw new ApiRequestException(ACTIVITY_ACCESS_DENIED, HttpStatus.FORBIDDEN);
        }

        activityRepository.delete(activity);
    }

    @Override
    @Transactional
    public void likeActivity(Long activityId, Long userId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND));
        User user = userService.getUserById(userId);

        if (activity.getLikedBy().contains(user)) {
            throw new ApiRequestException("Activity already liked", HttpStatus.BAD_REQUEST);
        }

        activity.getLikedBy().add(user);
        activityRepository.save(activity);

        notificationService.createNotification(
                activity.getUser(),
                user,
                NotificationType.LIKE,
                user.getFirstName() + " " + user.getLastName() + " liked your activity \"" + activity.getTitle() + "\"",
                activityId,
                "ACTIVITY"
        );
    }

    @Override
    @Transactional
    public void unlikeActivity(Long activityId, Long userId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND));
        User user = userService.getUserById(userId);

        if (!activity.getLikedBy().contains(user)) {
            throw new ApiRequestException("Activity not liked", HttpStatus.BAD_REQUEST);
        }

        activity.getLikedBy().remove(user);
        activityRepository.save(activity);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLikedByUser(Long activityId, Long userId) {
        return activityRepository.isLikedByUser(activityId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityStatsResponse getMonthlyStats(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusSeconds(1);

        LocalDateTime startOfPrevMonth = startOfMonth.minusMonths(1);
        LocalDateTime endOfPrevMonth = startOfMonth.minusSeconds(1);

        // Single native query to get all aggregated stats for the current month
        Object[] rawStats = activityRepository.getAggregatedStatsByUserAndDateRange(userId, startOfMonth, endOfMonth);
        // Native query returns Object[] where each element is a column value
        Object[] stats = (rawStats != null && rawStats.length > 0 && rawStats[0] instanceof Object[])
                ? (Object[]) rawStats[0] : rawStats;
        double totalDistance = stats != null ? ((Number) stats[0]).doubleValue() : 0.0;
        long totalDuration = stats != null ? ((Number) stats[1]).longValue() : 0L;
        int totalCalories = stats != null ? ((Number) stats[2]).intValue() : 0;
        int totalElevation = stats != null ? ((Number) stats[3]).intValue() : 0;
        int totalActivities = stats != null ? ((Number) stats[4]).intValue() : 0;

        Object[] prevRawStats = activityRepository.getAggregatedStatsByUserAndDateRange(userId, startOfPrevMonth, endOfPrevMonth);
        Object[] prevStats = (prevRawStats != null && prevRawStats.length > 0 && prevRawStats[0] instanceof Object[])
                ? (Object[]) prevRawStats[0] : prevRawStats;
        double prevDistance = prevStats != null ? ((Number) prevStats[0]).doubleValue() : 0.0;
        long prevDuration = prevStats != null ? ((Number) prevStats[1]).longValue() : 0L;
        int prevCalories = prevStats != null ? ((Number) prevStats[2]).intValue() : 0;
        int prevActivities = prevStats != null ? ((Number) prevStats[4]).intValue() : 0;

        String trend = calculateTrend(totalDistance, prevDistance);

        return ActivityStatsResponse.builder()
                .totalDistance(totalDistance)
                .totalDuration(totalDuration)
                .totalCalories(totalCalories)
                .totalActivities(totalActivities)
                .totalElevationGain(totalElevation)
                .averageDistance(totalActivities > 0 ? totalDistance / totalActivities : 0.0)
                .averageDuration(totalActivities > 0 ? (double) totalDuration / totalActivities : 0.0)
                .trendPercentage(trend)
                .distanceTrendPercentage(calculateTrendNumeric(totalDistance, prevDistance))
                .durationTrendPercentage(calculateTrendNumeric((double) totalDuration, (double) prevDuration))
                .caloriesTrendPercentage(calculateTrendNumeric((double) totalCalories, (double) prevCalories))
                .activitiesTrendPercentage(calculateTrendNumeric((double) totalActivities, (double) prevActivities))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public WeeklyStatsResponse getWeeklyStats(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfWeek = startOfWeek.plusWeeks(1).minusSeconds(1);

        LocalDateTime startOfPrevWeek = startOfWeek.minusWeeks(1);
        LocalDateTime endOfPrevWeek = startOfWeek.minusSeconds(1);

        Double prevWeekDistance = activityRepository.sumDistanceByUserAndDateRange(userId, startOfPrevWeek, endOfPrevWeek)
                .orElse(0.0);

        // Single query to get daily aggregated stats for the whole week
        List<Object[]> dailyResults = activityRepository.getDailyStatsByUserAndDateRange(userId, startOfWeek, endOfWeek);

        // PostgreSQL EXTRACT(DOW FROM ...) returns: 0=Sunday, 1=Monday, ..., 6=Saturday
        // Map to our array index: Mon=0, Tue=1, ..., Sun=6
        // So DOW 1->0, 2->1, 3->2, 4->3, 5->4, 6->5, 0->6
        Double[] distances = new Double[7];
        Integer[] durations = new Integer[7];
        Integer[] activities = new Integer[7];
        for (int i = 0; i < 7; i++) {
            distances[i] = 0.0;
            durations[i] = 0;
            activities[i] = 0;
        }

        double totalDistance = 0.0;
        for (Object[] row : dailyResults) {
            int dow = ((Number) row[0]).intValue(); // PostgreSQL DOW: 0=Sun, 1=Mon, ..., 6=Sat
            int index = dow == 0 ? 6 : dow - 1; // Convert to Mon=0 .. Sun=6
            double dist = ((Number) row[1]).doubleValue();
            int dur = ((Number) row[2]).intValue();
            int act = ((Number) row[3]).intValue();
            distances[index] = dist;
            durations[index] = dur;
            activities[index] = act;
            totalDistance += dist;
        }

        String trend = calculateTrend(totalDistance, prevWeekDistance);

        String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        List<WeeklyStatsResponse.DailyStats> dailyStats = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            dailyStats.add(WeeklyStatsResponse.DailyStats.builder()
                    .dayName(dayNames[i])
                    .distance(distances[i])
                    .duration(durations[i])
                    .activities(activities[i])
                    .build());
        }

        return WeeklyStatsResponse.builder()
                .totalDistance(totalDistance)
                .trendPercentage(trend)
                .dailyStats(dailyStats)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public int countActivitiesByUserId(Long userId) {
        return activityRepository.countByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Activity> searchUserActivities(Long userId, String searchText, ActivityType activityType, Pageable pageable) {
        if (activityType != null) {
            return activityRepository.searchUserActivitiesByType(userId, searchText, activityType, pageable);
        }
        return activityRepository.searchUserActivities(userId, searchText, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Activity> getActivitiesByUserIdAndType(Long userId, ActivityType activityType, Pageable pageable) {
        return activityRepository.findByUserIdAndActivityType(userId, activityType, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getUserTotalStats(Long userId) {
        int totalActivities = activityRepository.countByUserId(userId);
        Double totalDistance = activityRepository.sumDistanceByUserId(userId);
        Long totalDuration = activityRepository.sumDurationByUserId(userId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalActivities", totalActivities);
        stats.put("totalDistanceKm", totalDistance != null ? totalDistance : 0.0);
        stats.put("totalDurationSeconds", totalDuration != null ? totalDuration : 0L);
        return stats;
    }

    private Double calculateTrendNumeric(Double current, Double previous) {
        if (previous == null || previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        double percentage = ((current - previous) / previous) * 100;
        return Math.round(percentage * 10.0) / 10.0;
    }

    private String calculateTrend(Double current, Double previous) {
        if (previous == null || previous == 0) {
            return current > 0 ? "+100%" : "0%";
        }
        double percentage = ((current - previous) / previous) * 100;
        return String.format("%s%.1f%%", percentage >= 0 ? "+" : "", percentage);
    }
}
