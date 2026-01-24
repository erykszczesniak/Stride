package com.strava.backend.service.impl;

import com.strava.backend.domain.User;
import com.strava.backend.dto.user.UpdateUserRequest;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.repository.UserRepository;
import com.strava.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.strava.backend.constants.ErrorMessage.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    @Override
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    public Page<User> searchUsers(String searchText, Pageable pageable) {
        return userRepository.findAll(pageable); // Simplified, can use custom query
    }

    @Override
    @Transactional
    public User updateUser(Long userId, UpdateUserRequest request) {
        User user = getUserById(userId);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setBio(request.getBio());
        user.setCity(request.getCity());
        user.setCountry(request.getCountry());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void followUser(Long userId, Long followUserId) {
        User user = getUserById(userId);
        User followUser = getUserById(followUserId);

        if (userId.equals(followUserId)) {
            throw new ApiRequestException("Cannot follow yourself", HttpStatus.BAD_REQUEST);
        }

        if (followUser.getFollowers().contains(user)) {
            throw new ApiRequestException("Already following this user", HttpStatus.BAD_REQUEST);
        }

        followUser.getFollowers().add(user);
        userRepository.save(followUser);
    }

    @Override
    @Transactional
    public void unfollowUser(Long userId, Long unfollowUserId) {
        User user = getUserById(userId);
        User unfollowUser = getUserById(unfollowUserId);

        if (!unfollowUser.getFollowers().contains(user)) {
            throw new ApiRequestException("Not following this user", HttpStatus.BAD_REQUEST);
        }

        unfollowUser.getFollowers().remove(user);
        userRepository.save(unfollowUser);
    }

    @Override
    public List<User> getFollowers(Long userId) {
        User user = getUserById(userId);
        return new ArrayList<>(user.getFollowers());
    }

    @Override
    public List<User> getFollowing(Long userId) {
        User user = getUserById(userId);
        return new ArrayList<>(user.getFollowing());
    }

    @Override
    public boolean isFollowing(Long userId, Long followerId) {
        return userRepository.isFollowing(userId, followerId);
    }
}
