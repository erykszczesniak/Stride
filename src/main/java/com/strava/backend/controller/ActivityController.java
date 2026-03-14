package com.strava.backend.controller;

import com.strava.backend.dto.activity.*;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.mapper.ActivityMapper;
import com.strava.backend.security.UserPrincipal;
import com.strava.backend.service.ActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.strava.backend.constants.PathConstants.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(API_V1_ACTIVITIES)
public class ActivityController {

    private final ActivityMapper activityMapper;
    private final ActivityService activityService;

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) authentication.getPrincipal()).getId();
        }
        return null;
    }

    @GetMapping(ACTIVITY_ID)
    public ResponseEntity<FullActivityResponse> getActivityById(@PathVariable Long activityId) {
        Long currentUserId = getAuthenticatedUserId();
        return ResponseEntity.ok(activityMapper.getActivityById(activityId, currentUserId));
    }

    @GetMapping(FEED)
    public ResponseEntity<List<ActivityResponse>> getFeedActivities(
            @PageableDefault(size = 10) Pageable pageable) {
        Long currentUserId = getAuthenticatedUserId();
        var response = activityMapper.getFeedActivities(currentUserId, pageable);
        return ResponseEntity.ok()
                .headers(response.getHeaders())
                .body(response.getItems());
    }

    @PostMapping
    public ResponseEntity<ActivityResponse> createActivity(
            @Valid @RequestBody ActivityRequest request,
            BindingResult bindingResult) {
        Long currentUserId = getAuthenticatedUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(activityMapper.createActivity(currentUserId, request, bindingResult));
    }

    @PutMapping(ACTIVITY_ID)
    public ResponseEntity<ActivityResponse> updateActivity(
            @PathVariable Long activityId,
            @Valid @RequestBody ActivityRequest request,
            BindingResult bindingResult) {
        Long currentUserId = getAuthenticatedUserId();
        return ResponseEntity.ok(
                activityMapper.updateActivity(activityId, currentUserId, request, bindingResult));
    }

    @DeleteMapping(ACTIVITY_ID)
    public ResponseEntity<Void> deleteActivity(@PathVariable Long activityId) {
        Long currentUserId = getAuthenticatedUserId();
        activityMapper.deleteActivity(activityId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(ACTIVITY_ID + LIKE)
    public ResponseEntity<Map<String, String>> likeActivity(@PathVariable Long activityId) {
        Long currentUserId = getAuthenticatedUserId();
        activityService.likeActivity(activityId, currentUserId);
        return ResponseEntity.ok(Map.of("message", "Activity liked"));
    }

    @DeleteMapping(ACTIVITY_ID + LIKE)
    public ResponseEntity<Map<String, String>> unlikeActivity(@PathVariable Long activityId) {
        Long currentUserId = getAuthenticatedUserId();
        activityService.unlikeActivity(activityId, currentUserId);
        return ResponseEntity.ok(Map.of("message", "Activity unliked"));
    }

    @GetMapping(STATS + MONTHLY)
    public ResponseEntity<ActivityStatsResponse> getMonthlyStats() {
        Long currentUserId = getAuthenticatedUserId();
        return ResponseEntity.ok(activityMapper.getMonthlyStats(currentUserId));
    }

    @GetMapping(STATS + WEEKLY)
    public ResponseEntity<WeeklyStatsResponse> getWeeklyStats() {
        Long currentUserId = getAuthenticatedUserId();
        return ResponseEntity.ok(activityMapper.getWeeklyStats(currentUserId));
    }

    @GetMapping(USER + TARGET_USER_ID)
    public ResponseEntity<List<ActivityResponse>> getUserActivities(
            @PathVariable Long targetUserId,
            @PageableDefault(size = 10) Pageable pageable) {
        Long currentUserId = getAuthenticatedUserId();
        var response = activityMapper.getActivitiesByUserId(targetUserId, currentUserId, pageable);
        return ResponseEntity.ok()
                .headers(response.getHeaders())
                .body(response.getItems());
    }

    @GetMapping(MY)
    public ResponseEntity<List<ActivityResponse>> getMyActivities(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ActivityType activityType,
            @PageableDefault(size = 10) Pageable pageable) {
        Long currentUserId = getAuthenticatedUserId();

        if (search != null && !search.trim().isEmpty()) {
            return ResponseEntity.ok(activityMapper.searchMyActivities(currentUserId, search.trim(), activityType, pageable));
        }

        if (activityType != null) {
            return ResponseEntity.ok(activityMapper.getActivitiesByType(currentUserId, activityType, pageable));
        }

        var response = activityMapper.getActivitiesByUserId(currentUserId, currentUserId, pageable);
        return ResponseEntity.ok()
                .headers(response.getHeaders())
                .body(response.getItems());
    }

    @GetMapping(MY + STATS)
    public ResponseEntity<Map<String, Object>> getMyTotalStats() {
        Long currentUserId = getAuthenticatedUserId();
        return ResponseEntity.ok(activityMapper.getUserTotalStats(currentUserId));
    }

    @GetMapping(BY_TYPE)
    public ResponseEntity<List<ActivityResponse>> getActivitiesByType(
            @RequestParam ActivityType activityType,
            @PageableDefault(size = 10) Pageable pageable) {
        Long currentUserId = getAuthenticatedUserId();
        return ResponseEntity.ok(activityMapper.getActivitiesByType(currentUserId, activityType, pageable));
    }

    @GetMapping(PUBLIC)
    public ResponseEntity<List<ActivityResponse>> getPublicActivities(
            @PageableDefault(size = 10) Pageable pageable) {
        Long currentUserId = getAuthenticatedUserId();
        return ResponseEntity.ok(activityMapper.getPublicActivities(currentUserId, pageable));
    }
}
