package com.strava.backend.controller;

import com.strava.backend.dto.HeaderResponse;
import com.strava.backend.dto.user.BaseUserResponse;
import com.strava.backend.dto.user.UpdateUserRequest;
import com.strava.backend.dto.user.UserResponse;
import com.strava.backend.mapper.CommonMapper;
import com.strava.backend.mapper.UserMapper;
import com.strava.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
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

    private static final Long DEFAULT_USER_ID = 1L;

    private final UserMapper userMapper;
    private final UserService userService;
    private final CommonMapper commonMapper;

    @GetMapping(ME)
    public ResponseEntity<UserResponse> getCurrentUser(
            @RequestParam(required = false) Long userId) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
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
            @RequestParam(required = false) Long userId,
            @Valid @RequestBody UpdateUserRequest request,
            BindingResult bindingResult) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
        return ResponseEntity.ok(userMapper.updateUser(currentUserId, request, bindingResult));
    }

    @PostMapping(USER_ID + FOLLOW)
    public ResponseEntity<Map<String, String>> followUser(
            @PathVariable Long userId,
            @RequestParam(required = false) Long currentUserId) {
        Long followerUserId = currentUserId != null ? currentUserId : DEFAULT_USER_ID;
        userService.followUser(followerUserId, userId);
        return ResponseEntity.ok(Map.of("message", "User followed"));
    }

    @DeleteMapping(USER_ID + FOLLOW)
    public ResponseEntity<Map<String, String>> unfollowUser(
            @PathVariable Long userId,
            @RequestParam(required = false) Long currentUserId) {
        Long followerUserId = currentUserId != null ? currentUserId : DEFAULT_USER_ID;
        userService.unfollowUser(followerUserId, userId);
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
}
