package com.strava.activityservice.activity;

import com.strava.activityservice.domain.ActivityType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Public /api/v1/activities surface, mirroring main's ActivityController.
 * /feed, /public, /{id}/like and /{id}/comments/** are owned by
 * social-service — the gateway routes them there, so they don't exist here.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/activities")
public class ActivityController {

    private final ActivityMapper activityMapper;
    private final ActivityService activityService;

    @GetMapping("/{activityId}")
    public ResponseEntity<FullActivityResponse> getActivityById(
            @AuthenticationPrincipal Long currentUserId,
            @PathVariable Long activityId) {
        return ResponseEntity.ok(activityMapper.getActivityById(activityId, currentUserId));
    }

    @PostMapping
    public ResponseEntity<ActivityResponse> createActivity(
            @AuthenticationPrincipal Long currentUserId,
            @Valid @RequestBody ActivityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(activityMapper.createActivity(currentUserId, request));
    }

    @PutMapping("/{activityId}")
    public ResponseEntity<ActivityResponse> updateActivity(
            @AuthenticationPrincipal Long currentUserId,
            @PathVariable Long activityId,
            @Valid @RequestBody ActivityRequest request) {
        return ResponseEntity.ok(
                activityMapper.updateActivity(activityId, currentUserId, request));
    }

    @DeleteMapping("/{activityId}")
    public ResponseEntity<Void> deleteActivity(
            @AuthenticationPrincipal Long currentUserId,
            @PathVariable Long activityId) {
        activityMapper.deleteActivity(activityId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats/monthly")
    public ResponseEntity<ActivityStatsResponse> getMonthlyStats(@AuthenticationPrincipal Long currentUserId) {
        return ResponseEntity.ok(activityMapper.getMonthlyStats(currentUserId));
    }

    @GetMapping("/stats/weekly")
    public ResponseEntity<WeeklyStatsResponse> getWeeklyStats(@AuthenticationPrincipal Long currentUserId) {
        return ResponseEntity.ok(activityMapper.getWeeklyStats(currentUserId));
    }

    @GetMapping("/user/{targetUserId}")
    public ResponseEntity<List<ActivityResponse>> getUserActivities(
            @AuthenticationPrincipal Long currentUserId,
            @PathVariable Long targetUserId,
            @PageableDefault(size = 10) Pageable pageable) {
        var response = activityMapper.getActivitiesByUserId(targetUserId, currentUserId, pageable);
        return ResponseEntity.ok()
                .headers(response.getHeaders())
                .body(response.getItems());
    }

    @GetMapping("/my")
    public ResponseEntity<List<ActivityResponse>> getMyActivities(
            @AuthenticationPrincipal Long currentUserId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ActivityType activityType,
            @PageableDefault(size = 10) Pageable pageable) {

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

    @GetMapping("/my/stats")
    public ResponseEntity<Map<String, Object>> getMyTotalStats(@AuthenticationPrincipal Long currentUserId) {
        return ResponseEntity.ok(activityMapper.getUserTotalStats(currentUserId));
    }

    @GetMapping("/by-type")
    public ResponseEntity<List<ActivityResponse>> getActivitiesByType(
            @AuthenticationPrincipal Long currentUserId,
            @RequestParam ActivityType activityType,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(activityMapper.getActivitiesByType(currentUserId, activityType, pageable));
    }

    @PostMapping("/{activityId}/photos")
    public ResponseEntity<List<String>> uploadPhotos(
            @AuthenticationPrincipal Long currentUserId,
            @PathVariable Long activityId,
            @RequestParam("files") List<MultipartFile> files) {
        List<String> photos = activityService.uploadPhotos(activityId, currentUserId, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(photos);
    }

    @DeleteMapping("/{activityId}/photos/{photoIndex}")
    public ResponseEntity<Void> deletePhoto(
            @AuthenticationPrincipal Long currentUserId,
            @PathVariable Long activityId,
            @PathVariable int photoIndex) {
        activityService.deletePhoto(activityId, currentUserId, photoIndex);
        return ResponseEntity.noContent().build();
    }
}
