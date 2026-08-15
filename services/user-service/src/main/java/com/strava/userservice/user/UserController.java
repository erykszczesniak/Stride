package com.strava.userservice.user;

import com.strava.userservice.common.HeaderResponse;
import com.strava.userservice.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Public {@code /api/v1/users} contract, mirrored endpoint-for-endpoint from the
 * monolith's {@code UserController}. Identity arrives via {@code X-User-Id} injected by
 * the gateway (see {@code HeaderAuthenticationFilter}).
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(userService.getUserResponse(userId));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserResponse(userId));
    }

    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(
            @PathVariable Long userId,
            @AuthenticationPrincipal Long currentUserId) {
        return ResponseEntity.ok(userService.getUserProfile(userId, currentUserId));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<User> users = userService.getAllUsers(pageable);
        HeaderResponse<UserResponse> response = HeaderResponse.of(
                users.getContent().stream().map(UserResponse::from).toList(),
                users.getTotalPages(),
                users.getTotalElements());
        return ResponseEntity.ok()
                .headers(response.headers())
                .body(response.items());
    }

    /**
     * Full-text user search backed by Elasticsearch, falling back to the SQL
     * {@code ILIKE} implementation when ES is unavailable — same behavior and response
     * shape as the monolith (plain list body, no paging headers).
     */
    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> searchUsers(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<User> users;
        try {
            users = userService.searchUsers(query, pageable);
        } catch (Exception e) {
            log.warn("Elasticsearch user search failed, falling back to SQL ILIKE: {}", e.getMessage());
            users = userService.searchUsersFallback(query, pageable);
        }
        return ResponseEntity.ok(users.getContent().stream().map(UserResponse::from).toList());
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateUserRequest request) {
        userService.updateUser(userId, request);
        return ResponseEntity.ok(userService.getUserResponse(userId));
    }

    @PostMapping("/{userId}/follow")
    public ResponseEntity<Map<String, String>> followUser(
            @AuthenticationPrincipal Long currentUserId,
            @PathVariable Long userId) {
        userService.followUser(currentUserId, userId);
        return ResponseEntity.ok(Map.of("message", "User followed"));
    }

    @DeleteMapping("/{userId}/follow")
    public ResponseEntity<Map<String, String>> unfollowUser(
            @AuthenticationPrincipal Long currentUserId,
            @PathVariable Long userId) {
        userService.unfollowUser(currentUserId, userId);
        return ResponseEntity.ok(Map.of("message", "User unfollowed"));
    }

    @GetMapping("/{userId}/followers")
    public ResponseEntity<List<BaseUserResponse>> getFollowers(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getFollowers(userId).stream()
                .map(BaseUserResponse::from)
                .toList());
    }

    @GetMapping("/{userId}/following")
    public ResponseEntity<List<BaseUserResponse>> getFollowing(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getFollowing(userId).stream()
                .map(BaseUserResponse::from)
                .toList());
    }

    @GetMapping("/suggested")
    public ResponseEntity<List<BaseUserResponse>> getSuggestedUsers(
            @AuthenticationPrincipal Long currentUserId,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(userService.getSuggestedUsers(currentUserId, limit).stream()
                .map(BaseUserResponse::from)
                .toList());
    }

    @PutMapping("/me/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, String> request) {
        userService.changePassword(userId, request.get("currentPassword"), request.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Map<String, String>> deleteAccount(@AuthenticationPrincipal Long userId) {
        userService.deleteAccount(userId);
        return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
    }
}
