package com.strava.backend.mapper;

import com.strava.backend.domain.User;
import com.strava.backend.dto.HeaderResponse;
import com.strava.backend.dto.user.BaseUserResponse;
import com.strava.backend.dto.user.UpdateUserRequest;
import com.strava.backend.dto.user.UserResponse;
import com.strava.backend.exception.InputFieldException;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.repository.UserRepository;
import com.strava.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final CommonMapper commonMapper;
    private final UserService userService;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;

    public UserResponse getUserById(Long userId) {
        User user = userService.getUserById(userId);
        return mapToUserResponse(user);
    }

    public UserResponse getCurrentUser(Long userId) {
        User user = userService.getUserById(userId);
        return mapToUserResponse(user);
    }

    public HeaderResponse<UserResponse> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        return commonMapper.getHeaderResponse(users, UserResponse.class);
    }

    public UserResponse updateUser(Long userId, UpdateUserRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new InputFieldException(bindingResult);
        }
        User user = userService.updateUser(userId, request);
        return mapToUserResponse(user);
    }

    public BaseUserResponse getBaseUserResponse(User user) {
        return commonMapper.convertToResponse(user, BaseUserResponse.class);
    }

    private UserResponse mapToUserResponse(User user) {
        UserResponse response = commonMapper.convertToResponse(user, UserResponse.class);
        response.setFollowersCount(userRepository.countFollowers(user.getId()));
        response.setFollowingCount(userRepository.countFollowing(user.getId()));
        response.setActivitiesCount(activityRepository.countByUserId(user.getId()));
        return response;
    }
}
