package com.strava.backend.service;

import com.strava.backend.domain.Activity;
import com.strava.backend.dto.activity.ActivityRequest;
import com.strava.backend.dto.activity.ActivityStatsResponse;
import com.strava.backend.dto.activity.WeeklyStatsResponse;
import com.strava.backend.enums.ActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface ActivityService {

    Activity getActivityById(Long activityId);

    Page<Activity> getActivitiesByUserId(Long userId, Pageable pageable);

    List<Activity> getRecentActivitiesByUserId(Long userId, int limit);

    Page<Activity> getFeedActivities(Long userId, Pageable pageable);

    Page<Activity> getPublicActivities(Pageable pageable);

    Activity createActivity(Long userId, ActivityRequest request);

    Activity updateActivity(Long activityId, Long userId, ActivityRequest request);

    void deleteActivity(Long activityId, Long userId);

    void likeActivity(Long activityId, Long userId);

    void unlikeActivity(Long activityId, Long userId);

    boolean isLikedByUser(Long activityId, Long userId);

    ActivityStatsResponse getMonthlyStats(Long userId);

    WeeklyStatsResponse getWeeklyStats(Long userId);

    int countActivitiesByUserId(Long userId);

    Page<Activity> searchUserActivities(Long userId, String searchText, ActivityType activityType, Pageable pageable);

    Page<Activity> getActivitiesByUserIdAndType(Long userId, ActivityType activityType, Pageable pageable);

    Map<String, Object> getUserTotalStats(Long userId);

    List<String> uploadPhotos(Long activityId, Long userId, List<MultipartFile> files);

    void deletePhoto(Long activityId, Long userId, int photoIndex);
}
