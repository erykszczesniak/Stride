package com.strava.backend.service;

import com.strava.backend.domain.User;
import com.strava.backend.dto.user.UpdateUserRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {

    User getUserById(Long userId);

    User getUserByEmail(String email);

    Page<User> getAllUsers(Pageable pageable);

    Page<User> searchUsers(String searchText, Pageable pageable);

    User updateUser(Long userId, UpdateUserRequest request);

    void followUser(Long userId, Long followUserId);

    void unfollowUser(Long userId, Long unfollowUserId);

    List<User> getFollowers(Long userId);

    List<User> getFollowing(Long userId);

    boolean isFollowing(Long userId, Long followerId);
}
