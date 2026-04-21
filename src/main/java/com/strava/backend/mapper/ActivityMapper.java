package com.strava.backend.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.backend.domain.Activity;
import com.strava.backend.domain.User;
import com.strava.backend.dto.HeaderResponse;
import com.strava.backend.dto.activity.*;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.exception.InputFieldException;
import com.strava.backend.repository.UserRepository;
import com.strava.backend.service.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityMapper {

    private final CommonMapper commonMapper;
    private final ActivityService activityService;
    private final UserRepository userRepository;
    private final GearMapper gearMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FullActivityResponse getActivityById(Long activityId, Long currentUserId) {
        Activity activity = activityService.getActivityById(activityId);
        return mapToFullActivityResponse(activity, currentUserId);
    }

    public HeaderResponse<ActivityResponse> getActivitiesByUserId(Long userId, Long currentUserId, Pageable pageable) {
        // Respect private-profile flag: non-owner, non-follower viewers see nothing.
        if (currentUserId == null || !currentUserId.equals(userId)) {
            User target = userRepository.findById(userId).orElse(null);
            if (target != null && Boolean.TRUE.equals(target.getIsPrivate())) {
                boolean isFollower = currentUserId != null
                        && userRepository.isFollowing(userId, currentUserId);
                if (!isFollower) {
                    Page<Activity> empty = new PageImpl<>(List.of(), pageable, 0);
                    return HeaderResponse.of(List.of(), empty.getTotalPages(), empty.getTotalElements());
                }
            }
        }
        Page<Activity> activities = activityService.getActivitiesByUserId(userId, pageable);
        List<ActivityResponse> responses = activities.getContent().stream()
                .map(activity -> mapToActivityResponse(activity, currentUserId))
                .collect(Collectors.toList());
        return HeaderResponse.of(responses, activities.getTotalPages(), activities.getTotalElements());
    }

    public HeaderResponse<ActivityResponse> getFeedActivities(Long userId, Pageable pageable) {
        Page<Activity> activities = activityService.getFeedActivities(userId, pageable);
        List<ActivityResponse> responses = activities.getContent().stream()
                .map(activity -> mapToActivityResponse(activity, userId))
                .collect(Collectors.toList());
        return HeaderResponse.of(responses, activities.getTotalPages(), activities.getTotalElements());
    }

    public ActivityResponse createActivity(Long userId, ActivityRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new InputFieldException(bindingResult);
        }
        Activity activity = activityService.createActivity(userId, request);
        return mapToActivityResponse(activity, userId);
    }

    public ActivityResponse updateActivity(Long activityId, Long userId, ActivityRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new InputFieldException(bindingResult);
        }
        Activity activity = activityService.updateActivity(activityId, userId, request);
        return mapToActivityResponse(activity, userId);
    }

    public void deleteActivity(Long activityId, Long userId) {
        activityService.deleteActivity(activityId, userId);
    }

    public ActivityStatsResponse getMonthlyStats(Long userId) {
        return activityService.getMonthlyStats(userId);
    }

    public WeeklyStatsResponse getWeeklyStats(Long userId) {
        return activityService.getWeeklyStats(userId);
    }

    public List<ActivityResponse> searchMyActivities(Long userId, String searchText, ActivityType activityType, Pageable pageable) {
        Page<Activity> activities = activityService.searchUserActivities(userId, searchText, activityType, pageable);
        return activities.getContent().stream()
                .map(activity -> mapToActivityResponse(activity, userId))
                .collect(Collectors.toList());
    }

    public List<ActivityResponse> getActivitiesByType(Long userId, ActivityType activityType, Pageable pageable) {
        Page<Activity> activities = activityService.getActivitiesByUserIdAndType(userId, activityType, pageable);
        return activities.getContent().stream()
                .map(activity -> mapToActivityResponse(activity, userId))
                .collect(Collectors.toList());
    }

    public Map<String, Object> getUserTotalStats(Long userId) {
        return activityService.getUserTotalStats(userId);
    }

    public List<ActivityResponse> getPublicActivities(Long currentUserId, Pageable pageable) {
        Page<Activity> activities = activityService.getPublicActivitiesVisibleTo(currentUserId, pageable);
        return activities.getContent().stream()
                .map(activity -> mapToActivityResponse(activity, currentUserId))
                .collect(Collectors.toList());
    }

    private ActivityResponse mapToActivityResponse(Activity activity, Long currentUserId) {
        ActivityResponse response = commonMapper.convertToResponse(activity, ActivityResponse.class);
        response.setLikesCount(activity.getLikesCount());
        response.setCommentsCount(activity.getCommentsCount());
        response.setLikedByCurrentUser(activityService.isLikedByUser(activity.getId(), currentUserId));
        response.setGear(gearMapper.toSummary(activity.getGear()));
        response.setElevationProfile(parseElevationProfile(activity.getElevationProfile()));
        response.setPointTimestamps(parsePointTimestamps(activity.getPointTimestamps()));
        return response;
    }

    private FullActivityResponse mapToFullActivityResponse(Activity activity, Long currentUserId) {
        FullActivityResponse response = commonMapper.convertToResponse(activity, FullActivityResponse.class);
        response.setLikesCount(activity.getLikesCount());
        response.setCommentsCount(activity.getCommentsCount());
        response.setLikedByCurrentUser(activityService.isLikedByUser(activity.getId(), currentUserId));
        response.setGear(gearMapper.toSummary(activity.getGear()));
        response.setElevationProfile(parseElevationProfile(activity.getElevationProfile()));
        response.setPointTimestamps(parsePointTimestamps(activity.getPointTimestamps()));

        List<CommentResponse> comments = activity.getComments().stream()
                .map(comment -> commonMapper.convertToResponse(comment, CommentResponse.class))
                .collect(Collectors.toList());
        response.setComments(comments);

        return response;
    }

    /**
     * Deserialize the JSON-stringified elevation array from the DB column.
     * Returns null when the column is null/blank or malformed — the frontend
     * renders nothing for a null profile, so a parse failure degrades gracefully.
     * Null entries in the JSON (unknown altitude at that point) are coerced to 0.
     */
    private double[] parseElevationProfile(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            Double[] boxed = objectMapper.readValue(json, Double[].class);
            double[] result = new double[boxed.length];
            for (int i = 0; i < boxed.length; i++) {
                result[i] = boxed[i] != null ? boxed[i] : 0.0;
            }
            return result;
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse elevation profile JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Deserialize the JSON-stringified epoch-seconds timestamps array from the DB.
     * Returns null when the column is null/blank or malformed — the frontend falls
     * back to uniform-pace splits for a null timestamps series, so a parse failure
     * degrades gracefully (same path as legacy activities).
     */
    private long[] parsePointTimestamps(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            Long[] boxed = objectMapper.readValue(json, Long[].class);
            long[] result = new long[boxed.length];
            for (int i = 0; i < boxed.length; i++) {
                result[i] = boxed[i] != null ? boxed[i] : 0L;
            }
            return result;
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse point timestamps JSON: {}", e.getMessage());
            return null;
        }
    }
}
