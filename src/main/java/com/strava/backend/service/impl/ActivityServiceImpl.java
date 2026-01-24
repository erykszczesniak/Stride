package com.strava.backend.service.impl;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.User;
import com.strava.backend.dto.activity.ActivityRequest;
import com.strava.backend.dto.activity.ActivityStatsResponse;
import com.strava.backend.dto.activity.WeeklyStatsResponse;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.service.ActivityService;
import com.strava.backend.service.UserService;
import lombok.RequiredArgsConstructor;
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
import java.util.List;
import java.util.stream.Collectors;

import static com.strava.backend.constants.ErrorMessage.ACTIVITY_ACCESS_DENIED;
import static com.strava.backend.constants.ErrorMessage.ACTIVITY_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;
    private final UserService userService;

    @Override
    public Activity getActivityById(Long activityId) {
        return activityRepository.findById(activityId)
                .orElseThrow(() -> new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    @Override
    public Page<Activity> getActivitiesByUserId(Long userId, Pageable pageable) {
        return activityRepository.findAll(pageable); // Simplified
    }

    @Override
    public List<Activity> getRecentActivitiesByUserId(Long userId, int limit) {
        return activityRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public Page<Activity> getFeedActivities(Long userId, Pageable pageable) {
        User user = userService.getUserById(userId);
        List<Long> followingIds = user.getFollowing().stream()
                .map(User::getId)
                .collect(Collectors.toList());
        followingIds.add(userId);
        return activityRepository.findAll(pageable); // Simplified
    }

    @Override
    public Page<Activity> getPublicActivities(Pageable pageable) {
        return activityRepository.findAll(pageable);
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
        Activity activity = getActivityById(activityId);

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
        Activity activity = getActivityById(activityId);

        if (!activity.getUser().getId().equals(userId)) {
            throw new ApiRequestException(ACTIVITY_ACCESS_DENIED, HttpStatus.FORBIDDEN);
        }

        activityRepository.delete(activity);
    }

    @Override
    @Transactional
    public void likeActivity(Long activityId, Long userId) {
        Activity activity = getActivityById(activityId);
        User user = userService.getUserById(userId);

        if (activity.getLikedBy().contains(user)) {
            throw new ApiRequestException("Activity already liked", HttpStatus.BAD_REQUEST);
        }

        activity.getLikedBy().add(user);
        activityRepository.save(activity);
    }

    @Override
    @Transactional
    public void unlikeActivity(Long activityId, Long userId) {
        Activity activity = getActivityById(activityId);
        User user = userService.getUserById(userId);

        if (!activity.getLikedBy().contains(user)) {
            throw new ApiRequestException("Activity not liked", HttpStatus.BAD_REQUEST);
        }

        activity.getLikedBy().remove(user);
        activityRepository.save(activity);
    }

    @Override
    public boolean isLikedByUser(Long activityId, Long userId) {
        return activityRepository.isLikedByUser(activityId, userId);
    }

    @Override
    public ActivityStatsResponse getMonthlyStats(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusSeconds(1);

        LocalDateTime startOfPrevMonth = startOfMonth.minusMonths(1);
        LocalDateTime endOfPrevMonth = startOfMonth.minusSeconds(1);

        Double totalDistance = activityRepository.sumDistanceByUserAndDateRange(userId, startOfMonth, endOfMonth)
                .orElse(0.0);
        Long totalDuration = activityRepository.sumDurationByUserAndDateRange(userId, startOfMonth, endOfMonth)
                .orElse(0L);
        Integer totalCalories = activityRepository.sumCaloriesByUserAndDateRange(userId, startOfMonth, endOfMonth)
                .orElse(0);
        Integer totalElevation = activityRepository.sumElevationByUserAndDateRange(userId, startOfMonth, endOfMonth)
                .orElse(0);
        int totalActivities = activityRepository.countByUserAndDateRange(userId, startOfMonth, endOfMonth);

        Double prevMonthDistance = activityRepository.sumDistanceByUserAndDateRange(userId, startOfPrevMonth, endOfPrevMonth)
                .orElse(0.0);

        String trend = calculateTrend(totalDistance, prevMonthDistance);

        return ActivityStatsResponse.builder()
                .totalDistance(totalDistance)
                .totalDuration(totalDuration)
                .totalCalories(totalCalories)
                .totalActivities(totalActivities)
                .totalElevationGain(totalElevation)
                .averageDistance(totalActivities > 0 ? totalDistance / totalActivities : 0.0)
                .averageDuration(totalActivities > 0 ? (double) totalDuration / totalActivities : 0.0)
                .trendPercentage(trend)
                .build();
    }

    @Override
    public WeeklyStatsResponse getWeeklyStats(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfWeek = startOfWeek.plusWeeks(1).minusSeconds(1);

        LocalDateTime startOfPrevWeek = startOfWeek.minusWeeks(1);
        LocalDateTime endOfPrevWeek = startOfWeek.minusSeconds(1);

        Double totalDistance = activityRepository.sumDistanceByUserAndDateRange(userId, startOfWeek, endOfWeek)
                .orElse(0.0);
        Double prevWeekDistance = activityRepository.sumDistanceByUserAndDateRange(userId, startOfPrevWeek, endOfPrevWeek)
                .orElse(0.0);

        String trend = calculateTrend(totalDistance, prevWeekDistance);

        List<WeeklyStatsResponse.DailyStats> dailyStats = new ArrayList<>();
        String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

        for (int i = 0; i < 7; i++) {
            LocalDateTime dayStart = startOfWeek.plusDays(i);
            LocalDateTime dayEnd = dayStart.plusDays(1).minusSeconds(1);

            Double dayDistance = activityRepository.sumDistanceByUserAndDateRange(userId, dayStart, dayEnd)
                    .orElse(0.0);
            Long dayDuration = activityRepository.sumDurationByUserAndDateRange(userId, dayStart, dayEnd)
                    .orElse(0L);
            int dayActivities = activityRepository.countByUserAndDateRange(userId, dayStart, dayEnd);

            dailyStats.add(WeeklyStatsResponse.DailyStats.builder()
                    .dayName(dayNames[i])
                    .distance(dayDistance)
                    .duration(dayDuration.intValue())
                    .activities(dayActivities)
                    .build());
        }

        return WeeklyStatsResponse.builder()
                .totalDistance(totalDistance)
                .trendPercentage(trend)
                .dailyStats(dailyStats)
                .build();
    }

    @Override
    public int countActivitiesByUserId(Long userId) {
        return activityRepository.countByUserId(userId);
    }

    private String calculateTrend(Double current, Double previous) {
        if (previous == null || previous == 0) {
            return current > 0 ? "+100%" : "0%";
        }
        double percentage = ((current - previous) / previous) * 100;
        return String.format("%s%.1f%%", percentage >= 0 ? "+" : "", percentage);
    }
}
