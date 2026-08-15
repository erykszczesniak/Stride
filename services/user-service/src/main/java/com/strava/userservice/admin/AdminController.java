package com.strava.userservice.admin;

import com.strava.userservice.common.HeaderResponse;
import com.strava.userservice.domain.User;
import com.strava.userservice.domain.UserFollowRepository;
import com.strava.userservice.user.ActivityStatsPort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * User-side admin contract ({@code /api/v1/admin/users/**}), mirrored from the
 * monolith's {@code AdminController}. Admin STATS and admin ACTIVITIES belong to
 * web-bff / activity-service. ADMIN role is enforced in {@code SecurityConfig}
 * (roles arrive via {@code X-User-Roles}).
 *
 * <p>Parity note: list endpoints return the {@link HeaderResponse} envelope as the JSON
 * body (the frontend reads {@code response.data.items}), not as HTTP headers.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;
    private final UserFollowRepository followRepository;
    private final ActivityStatsPort activityStats;

    @GetMapping("/users")
    public ResponseEntity<HeaderResponse<AdminUserResponse>> getAllUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AdminUserResponse> users = adminService.getAllUsers(pageable).map(this::toResponse);
        return ResponseEntity.ok(HeaderResponse.of(
                users.getContent(),
                users.getTotalPages(),
                users.getTotalElements()));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<AdminUserResponse> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(toResponse(adminService.getUserById(userId)));
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<Map<String, String>> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        adminService.updateUserRole(userId, request.role());
        return ResponseEntity.ok(Map.of("message", "User role updated"));
    }

    @PutMapping("/users/{userId}/toggle-active")
    public ResponseEntity<Map<String, String>> toggleUserActive(@PathVariable Long userId) {
        adminService.toggleUserActive(userId);
        return ResponseEntity.ok(Map.of("message", "User active status toggled"));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.ok(Map.of("message", "User deleted"));
    }

    private AdminUserResponse toResponse(User user) {
        return AdminUserResponse.from(user,
                activityStats.countActivities(user.getId()),
                (int) followRepository.countByUserId(user.getId()),
                (int) followRepository.countByFollowerId(user.getId()));
    }
}
