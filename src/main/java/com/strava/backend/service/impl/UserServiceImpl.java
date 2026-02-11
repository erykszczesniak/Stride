package com.strava.backend.service.impl;

import com.strava.backend.domain.User;
import com.strava.backend.dto.user.UpdateUserRequest;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.repository.UserRepository;
import com.strava.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.strava.backend.constants.ErrorMessage.USER_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Cacheable(value = "users", key = "#userId")
    public User getUserById(Long userId) {
        log.debug("Cache miss - fetching user by id: {}", userId);
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
    @CacheEvict(value = "users", key = "#userId")
    public User updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setBio(request.getBio());
        user.setCity(request.getCity());
        user.setCountry(request.getCountry());
        user.setWeight(request.getWeight());
        user.setHeight(request.getHeight());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "users", key = "#userId"),
            @CacheEvict(value = "users", key = "#followUserId"),
            @CacheEvict(value = "userFollowers", key = "#followUserId"),
            @CacheEvict(value = "userFollowers", key = "'following-' + #userId")
    })
    public void followUser(Long userId, Long followUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));
        User followUser = userRepository.findById(followUserId)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));

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
    @Caching(evict = {
            @CacheEvict(value = "users", key = "#userId"),
            @CacheEvict(value = "users", key = "#unfollowUserId"),
            @CacheEvict(value = "userFollowers", key = "#unfollowUserId"),
            @CacheEvict(value = "userFollowers", key = "'following-' + #userId")
    })
    public void unfollowUser(Long userId, Long unfollowUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));
        User unfollowUser = userRepository.findById(unfollowUserId)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (!unfollowUser.getFollowers().contains(user)) {
            throw new ApiRequestException("Not following this user", HttpStatus.BAD_REQUEST);
        }

        unfollowUser.getFollowers().remove(user);
        userRepository.save(unfollowUser);
    }

    @Override
    @Cacheable(value = "userFollowers", key = "#userId")
    public List<User> getFollowers(Long userId) {
        log.debug("Cache miss - fetching followers for user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));
        return new ArrayList<>(user.getFollowers());
    }

    @Override
    @Cacheable(value = "userFollowers", key = "'following-' + #userId")
    public List<User> getFollowing(Long userId) {
        log.debug("Cache miss - fetching following for user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));
        return new ArrayList<>(user.getFollowing());
    }

    @Override
    public boolean isFollowing(Long userId, Long followerId) {
        return userRepository.isFollowing(userId, followerId);
    }

    @Override
    public List<User> getSuggestedUsers(Long userId, int limit) {
        return userRepository.findSuggestedUsers(userId, PageRequest.of(0, limit));
    }
}
