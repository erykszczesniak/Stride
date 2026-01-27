package com.strava.backend.controller;

import com.strava.backend.dto.activity.*;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.mapper.ActivityMapper;
import com.strava.backend.mapper.CommonMapper;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.service.ActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.strava.backend.constants.PathConstants.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(API_V1_ACTIVITIES)
public class ActivityController {

    private static final Long DEFAULT_USER_ID = 1L;

    private final ActivityMapper activityMapper;
    private final ActivityService activityService;
    private final ActivityRepository activityRepository;
    private final CommonMapper commonMapper;

    @GetMapping(ACTIVITY_ID)
    public ResponseEntity<FullActivityResponse> getActivityById(
            @PathVariable Long activityId,
            @RequestParam(required = false) Long userId) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
        return ResponseEntity.ok(activityMapper.getActivityById(activityId, currentUserId));
    }

    @GetMapping(FEED)
    public ResponseEntity<List<ActivityResponse>> getFeedActivities(
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 10) Pageable pageable) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
        var response = activityMapper.getFeedActivities(currentUserId, pageable);
        return ResponseEntity.ok()
                .headers(response.getHeaders())
                .body(response.getItems());
    }

    @PostMapping
    public ResponseEntity<ActivityResponse> createActivity(
            @RequestParam(required = false) Long userId,
            @Valid @RequestBody ActivityRequest request,
            BindingResult bindingResult) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(activityMapper.createActivity(currentUserId, request, bindingResult));
    }

    @PutMapping(ACTIVITY_ID)
    public ResponseEntity<ActivityResponse> updateActivity(
            @PathVariable Long activityId,
            @RequestParam(required = false) Long userId,
            @Valid @RequestBody ActivityRequest request,
            BindingResult bindingResult) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
        return ResponseEntity.ok(
                activityMapper.updateActivity(activityId, currentUserId, request, bindingResult));
    }

    @DeleteMapping(ACTIVITY_ID)
    public ResponseEntity<Void> deleteActivity(
            @PathVariable Long activityId,
            @RequestParam(required = false) Long userId) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
        activityMapper.deleteActivity(activityId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(ACTIVITY_ID + LIKE)
    public ResponseEntity<Map<String, String>> likeActivity(
            @PathVariable Long activityId,
            @RequestParam(required = false) Long userId) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
        activityService.likeActivity(activityId, currentUserId);
        return ResponseEntity.ok(Map.of("message", "Activity liked"));
    }

    @DeleteMapping(ACTIVITY_ID + LIKE)
    public ResponseEntity<Map<String, String>> unlikeActivity(
            @PathVariable Long activityId,
            @RequestParam(required = false) Long userId) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
        activityService.unlikeActivity(activityId, currentUserId);
        return ResponseEntity.ok(Map.of("message", "Activity unliked"));
    }

    @GetMapping(STATS + MONTHLY)
    public ResponseEntity<ActivityStatsResponse> getMonthlyStats(
            @RequestParam(required = false) Long userId) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
        return ResponseEntity.ok(activityMapper.getMonthlyStats(currentUserId));
    }

    @GetMapping(STATS + WEEKLY)
    public ResponseEntity<WeeklyStatsResponse> getWeeklyStats(
            @RequestParam(required = false) Long userId) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
        return ResponseEntity.ok(activityMapper.getWeeklyStats(currentUserId));
    }

    @GetMapping("/user/{targetUserId}")
    public ResponseEntity<List<ActivityResponse>> getUserActivities(
            @PathVariable Long targetUserId,
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 10) Pageable pageable) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
        var response = activityMapper.getActivitiesByUserId(targetUserId, currentUserId, pageable);
        return ResponseEntity.ok()
                .headers(response.getHeaders())
                .body(response.getItems());
    }

    @GetMapping("/my")
    public ResponseEntity<List<ActivityResponse>> getMyActivities(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ActivityType activityType,
            @PageableDefault(size = 10) Pageable pageable) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;

        if (search != null && !search.trim().isEmpty()) {
            var activities = activityRepository.searchUserActivities(currentUserId, search.trim(), pageable);
            var responses = activities.getContent().stream()
                    .filter(a -> activityType == null || a.getActivityType() == activityType)
                    .map(activity -> {
                        ActivityResponse response = commonMapper.convertToResponse(activity, ActivityResponse.class);
                        response.setLikesCount(activity.getLikesCount());
                        response.setCommentsCount(activity.getCommentsCount());
                        response.setLikedByCurrentUser(activityService.isLikedByUser(activity.getId(), currentUserId));
                        return response;
                    })
                    .toList();
            return ResponseEntity.ok(responses);
        }

        if (activityType != null) {
            var activities = activityRepository.findByUserIdAndActivityType(currentUserId, activityType, pageable);
            var responses = activities.getContent().stream()
                    .map(activity -> {
                        ActivityResponse response = commonMapper.convertToResponse(activity, ActivityResponse.class);
                        response.setLikesCount(activity.getLikesCount());
                        response.setCommentsCount(activity.getCommentsCount());
                        response.setLikedByCurrentUser(activityService.isLikedByUser(activity.getId(), currentUserId));
                        return response;
                    })
                    .toList();
            return ResponseEntity.ok(responses);
        }

        var response = activityMapper.getActivitiesByUserId(currentUserId, currentUserId, pageable);
        return ResponseEntity.ok()
                .headers(response.getHeaders())
                .body(response.getItems());
    }

    @GetMapping("/my/stats")
    public ResponseEntity<Map<String, Object>> getMyTotalStats(
            @RequestParam(required = false) Long userId) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
        int totalActivities = activityRepository.countByUserId(currentUserId);
        Double totalDistance = activityRepository.sumDistanceByUserId(currentUserId);
        Long totalDuration = activityRepository.sumDurationByUserId(currentUserId);

        return ResponseEntity.ok(Map.of(
                "totalActivities", totalActivities,
                "totalDistanceKm", totalDistance != null ? totalDistance : 0.0,
                "totalDurationSeconds", totalDuration != null ? totalDuration : 0L
        ));
    }

    @GetMapping("/by-type")
    public ResponseEntity<List<ActivityResponse>> getActivitiesByType(
            @RequestParam(required = false) Long userId,
            @RequestParam ActivityType activityType,
            @PageableDefault(size = 10) Pageable pageable) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
        var activities = activityRepository.findByUserIdAndActivityType(currentUserId, activityType, pageable);
        var responses = commonMapper.convertToResponseList(activities.getContent(), ActivityResponse.class);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/public")
    public ResponseEntity<List<ActivityResponse>> getPublicActivities(
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 10) Pageable pageable) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
        var activities = activityService.getPublicActivities(pageable);
        var responses = activities.getContent().stream()
                .map(activity -> {
                    ActivityResponse response = commonMapper.convertToResponse(activity, ActivityResponse.class);
                    response.setLikesCount(activity.getLikesCount());
                    response.setCommentsCount(activity.getCommentsCount());
                    response.setLikedByCurrentUser(activityService.isLikedByUser(activity.getId(), currentUserId));
                    return response;
                })
                .toList();
        return ResponseEntity.ok(responses);
    }
}
