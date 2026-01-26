package com.strava.backend.controller;

import com.strava.backend.dto.HeaderResponse;
import com.strava.backend.dto.admin.AdminActivityResponse;
import com.strava.backend.dto.admin.AdminStatsResponse;
import com.strava.backend.dto.admin.AdminUserResponse;
import com.strava.backend.dto.admin.UpdateUserRoleRequest;
import com.strava.backend.dto.user.BaseUserResponse;
import com.strava.backend.mapper.CommonMapper;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.repository.UserRepository;
import com.strava.backend.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.strava.backend.constants.PathConstants.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(API_V1_ADMIN)
public class AdminController {

    private final AdminService adminService;
    private final CommonMapper commonMapper;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;

    @GetMapping(STATS)
    public ResponseEntity<AdminStatsResponse> getAdminStats() {
        return ResponseEntity.ok(adminService.getAdminStats());
    }

    @GetMapping(USERS)
    public ResponseEntity<HeaderResponse<AdminUserResponse>> getAllUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        var users = adminService.getAllUsers(pageable);
        var response = users.map(user -> {
            AdminUserResponse userResponse = commonMapper.convertToResponse(user, AdminUserResponse.class);
            userResponse.setActivitiesCount(activityRepository.countByUserId(user.getId()));
            userResponse.setFollowersCount(userRepository.countFollowers(user.getId()));
            userResponse.setFollowingCount(userRepository.countFollowing(user.getId()));
            return userResponse;
        });
        return ResponseEntity.ok(HeaderResponse.of(
                response.getContent(),
                response.getTotalPages(),
                response.getTotalElements()));
    }

    @GetMapping(USERS + USER_ID)
    public ResponseEntity<AdminUserResponse> getUserById(@PathVariable Long userId) {
        var user = adminService.getUserById(userId);
        AdminUserResponse response = commonMapper.convertToResponse(user, AdminUserResponse.class);
        response.setActivitiesCount(activityRepository.countByUserId(userId));
        response.setFollowersCount(userRepository.countFollowers(userId));
        response.setFollowingCount(userRepository.countFollowing(userId));
        return ResponseEntity.ok(response);
    }

    @PutMapping(USERS + USER_ID + "/role")
    public ResponseEntity<Map<String, String>> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        adminService.updateUserRole(userId, request.getRole());
        return ResponseEntity.ok(Map.of("message", "User role updated"));
    }

    @PutMapping(USERS + USER_ID + "/toggle-active")
    public ResponseEntity<Map<String, String>> toggleUserActive(@PathVariable Long userId) {
        adminService.toggleUserActive(userId);
        return ResponseEntity.ok(Map.of("message", "User active status toggled"));
    }

    @DeleteMapping(USERS + USER_ID)
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.ok(Map.of("message", "User deleted"));
    }

    @GetMapping(ACTIVITIES)
    public ResponseEntity<HeaderResponse<AdminActivityResponse>> getAllActivities(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        var activities = (search != null && !search.isBlank())
                ? adminService.searchActivities(search, pageable)
                : adminService.getAllActivities(pageable);
        var response = activities.map(activity -> {
            AdminActivityResponse activityResponse = commonMapper.convertToResponse(activity, AdminActivityResponse.class);
            activityResponse.setUser(commonMapper.convertToResponse(activity.getUser(), BaseUserResponse.class));
            return activityResponse;
        });
        return ResponseEntity.ok(HeaderResponse.of(
                response.getContent(),
                response.getTotalPages(),
                response.getTotalElements()));
    }

    @DeleteMapping(ACTIVITIES + ACTIVITY_ID)
    public ResponseEntity<Map<String, String>> deleteActivity(@PathVariable Long activityId) {
        adminService.deleteActivity(activityId);
        return ResponseEntity.ok(Map.of("message", "Activity deleted"));
    }
}
