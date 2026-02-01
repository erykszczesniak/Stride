package com.strava.backend.controller;

import com.strava.backend.dto.HeaderResponse;
import com.strava.backend.dto.user.BaseUserResponse;
import com.strava.backend.dto.user.UpdateUserRequest;
import com.strava.backend.dto.user.UserResponse;
import com.strava.backend.mapper.CommonMapper;
import com.strava.backend.mapper.UserMapper;
import com.strava.backend.security.UserPrincipal;
import com.strava.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.strava.backend.constants.PathConstants.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(API_V1_USERS)
public class UserController {

    private final UserMapper userMapper;
    private final UserService userService;
    private final CommonMapper commonMapper;

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) authentication.getPrincipal()).getId();
        }
        return null;
    }

    @GetMapping(ME)
    public ResponseEntity<UserResponse> getCurrentUser() {
        Long currentUserId = getAuthenticatedUserId();
        return ResponseEntity.ok(userMapper.getCurrentUser(currentUserId));
    }

    @GetMapping(USER_ID)
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(userMapper.getUserById(userId));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        HeaderResponse<UserResponse> response = userMapper.getAllUsers(pageable);
        return ResponseEntity.ok()
                .headers(response.getHeaders())
                .body(response.getItems());
    }

    @PutMapping(ME)
    public ResponseEntity<UserResponse> updateCurrentUser(
            @Valid @RequestBody UpdateUserRequest request,
            BindingResult bindingResult) {
        Long currentUserId = getAuthenticatedUserId();
        return ResponseEntity.ok(userMapper.updateUser(currentUserId, request, bindingResult));
    }

    @PostMapping(USER_ID + FOLLOW)
    public ResponseEntity<Map<String, String>> followUser(@PathVariable Long userId) {
        Long currentUserId = getAuthenticatedUserId();
        userService.followUser(currentUserId, userId);
        return ResponseEntity.ok(Map.of("message", "User followed"));
    }

    @DeleteMapping(USER_ID + FOLLOW)
    public ResponseEntity<Map<String, String>> unfollowUser(@PathVariable Long userId) {
        Long currentUserId = getAuthenticatedUserId();
        userService.unfollowUser(currentUserId, userId);
        return ResponseEntity.ok(Map.of("message", "User unfollowed"));
    }

    @GetMapping(USER_ID + FOLLOWERS)
    public ResponseEntity<List<BaseUserResponse>> getFollowers(@PathVariable Long userId) {
        List<BaseUserResponse> followers = userService.getFollowers(userId).stream()
                .map(user -> commonMapper.convertToResponse(user, BaseUserResponse.class))
                .collect(Collectors.toList());
        return ResponseEntity.ok(followers);
    }

    @GetMapping(USER_ID + FOLLOWING)
    public ResponseEntity<List<BaseUserResponse>> getFollowing(@PathVariable Long userId) {
        List<BaseUserResponse> following = userService.getFollowing(userId).stream()
                .map(user -> commonMapper.convertToResponse(user, BaseUserResponse.class))
                .collect(Collectors.toList());
        return ResponseEntity.ok(following);
    }

    @GetMapping(SUGGESTED)
    public ResponseEntity<List<BaseUserResponse>> getSuggestedUsers(
            @RequestParam(defaultValue = "5") int limit) {
        Long currentUserId = getAuthenticatedUserId();
        List<BaseUserResponse> suggested = userService.getSuggestedUsers(currentUserId, limit).stream()
                .map(user -> commonMapper.convertToResponse(user, BaseUserResponse.class))
                .collect(Collectors.toList());
        return ResponseEntity.ok(suggested);
    }
}
