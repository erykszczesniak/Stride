package com.strava.activityservice.admin;

import com.strava.activityservice.activity.ActivityService;
import com.strava.activityservice.common.HeaderResponse;
import com.strava.activityservice.domain.Activity;
import com.strava.activityservice.domain.ActivityRepository;
import com.strava.activityservice.exception.ApiRequestException;
import com.strava.activityservice.user.BaseUserResponse;
import com.strava.activityservice.user.UserCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.strava.activityservice.constants.ErrorMessage.ACTIVITY_NOT_FOUND;

/**
 * Activity half of main's AdminController (the user half lives in
 * user-service). The list ships the serialized HeaderResponse envelope as
 * the body — exactly like main; ROLE_ADMIN is enforced both here and in
 * SecurityConfig's /api/v1/admin/** rule.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/activities")
@PreAuthorize("hasRole('ADMIN')")
public class AdminActivityController {

    private final ActivityRepository activityRepository;
    private final ActivityService activityService;
    private final UserCacheService userCacheService;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<HeaderResponse<AdminActivityResponse>> getAllActivities(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Activity> activities = (search != null && !search.isBlank())
                ? activityRepository.searchByTitle(search, pageable)
                : activityRepository.findAllByOrderByCreatedAtDesc(pageable);

        List<Long> userIds = activities.getContent().stream().map(Activity::getUserId).toList();
        Map<Long, BaseUserResponse> users = userCacheService.usersFor(userIds);

        List<AdminActivityResponse> items = activities.getContent().stream()
                .map(activity -> toResponse(activity, users.get(activity.getUserId())))
                .toList();
        return ResponseEntity.ok(HeaderResponse.of(
                items,
                activities.getTotalPages(),
                activities.getTotalElements()));
    }

    @DeleteMapping("/{activityId}")
    @Transactional
    public ResponseEntity<Map<String, String>> deleteActivity(@PathVariable Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND));
        activityService.deleteWithPersonalRecords(activity);
        return ResponseEntity.ok(Map.of("message", "Activity deleted"));
    }

    private AdminActivityResponse toResponse(Activity activity, BaseUserResponse user) {
        AdminActivityResponse response = new AdminActivityResponse();
        response.setId(activity.getId());
        response.setTitle(activity.getTitle());
        response.setActivityType(activity.getActivityType());
        response.setDistance(activity.getDistance());
        response.setDuration(activity.getDuration());
        response.setCalories(activity.getCalories());
        response.setStartTime(activity.getStartTime());
        response.setCreatedAt(activity.getCreatedAt());
        response.setUser(user);
        return response;
    }
}
