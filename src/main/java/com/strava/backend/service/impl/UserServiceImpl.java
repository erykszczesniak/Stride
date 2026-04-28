package com.strava.backend.service.impl;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.User;
import com.strava.backend.dto.activity.ActivityResponse;
import com.strava.backend.dto.user.BaseUserResponse;
import com.strava.backend.dto.user.UpdateUserRequest;
import com.strava.backend.dto.user.UserProfileResponse;
import com.strava.backend.enums.NotificationType;
import com.strava.backend.enums.AuthProvider;
import com.strava.backend.event.UserDeletedEvent;
import com.strava.backend.event.UserUpsertedEvent;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.mapper.CommonMapper;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.repository.UserRepository;
import com.strava.backend.search.UserDocument;
import com.strava.backend.search.UserSearchRepository;
import com.strava.backend.service.NotificationService;
import com.strava.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.strava.backend.constants.ErrorMessage.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final CommonMapper commonMapper;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final UserSearchRepository userSearchRepository;

    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> searchUsers(String searchText, Pageable pageable) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return userRepository.findAll(pageable);
        }
        Page<UserDocument> hits = userSearchRepository.searchByQuery(searchText.trim(), pageable);
        List<Long> ids = hits.getContent().stream().map(UserDocument::getId).toList();
        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, hits.getTotalElements());
        }
        // Preserve the Elasticsearch-scored order when hydrating from JPA.
        List<User> usersInDbOrder = userRepository.findByIdIn(ids);
        java.util.Map<Long, User> byId = new java.util.HashMap<>();
        for (User u : usersInDbOrder) {
            byId.put(u.getId(), u);
        }
        List<User> ordered = ids.stream()
                .map(byId::get)
                .filter(java.util.Objects::nonNull)
                .toList();
        return new PageImpl<>(ordered, pageable, hits.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> searchUsersFallback(String searchText, Pageable pageable) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return userRepository.findAll(pageable);
        }
        return userRepository.searchUsersByText(searchText.trim(), pageable);
    }

    @Override
    @Transactional
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
        if (request.getEmailNotifications() != null) {
            user.setEmailNotifications(request.getEmailNotifications());
        }
        if (request.getPushNotifications() != null) {
            user.setPushNotifications(request.getPushNotifications());
        }
        if (request.getWeeklySummary() != null) {
            user.setWeeklySummary(request.getWeeklySummary());
        }
        if (request.getIsPrivate() != null) {
            user.setIsPrivate(request.getIsPrivate());
        }
        if (request.getHideFromSearch() != null) {
            user.setHideFromSearch(request.getHideFromSearch());
        }
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new UserUpsertedEvent(saved));
        return saved;
    }

    @Override
    @Transactional
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

        notificationService.createNotification(
                followUser,
                user,
                NotificationType.FOLLOW,
                user.getFirstName() + " " + user.getLastName() + " started following you",
                null,
                null
        );
    }

    @Override
    @Transactional
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
    @Transactional(readOnly = true)
    public List<User> getFollowers(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));
        return new ArrayList<>(user.getFollowers());
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getFollowing(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));
        return new ArrayList<>(user.getFollowing());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFollowing(Long userId, Long followerId) {
        return userRepository.isFollowing(userId, followerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getSuggestedUsers(Long userId, int limit) {
        return userRepository.findSuggestedUsers(userId, PageRequest.of(0, limit));
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId, Long currentUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));

        boolean isPrivate = Boolean.TRUE.equals(user.getIsPrivate());
        boolean isSelf = currentUserId != null && currentUserId.equals(userId);
        boolean isFollower = currentUserId != null && !isSelf
                && userRepository.isFollowing(userId, currentUserId);
        boolean canViewActivities = !isPrivate || isSelf || isFollower;

        // Privacy short-circuit: if the profile is private and the viewer isn't
        // the owner or a follower, return a sanitized profile (name + avatar only).
        if (!canViewActivities) {
            return UserProfileResponse.builder()
                    .id(user.getId())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .avatar(user.getAvatar())
                    .recentActivities(List.of())
                    .isFollowedByCurrentUser(false)
                    .isPrivate(true)
                    .canViewActivities(false)
                    .build();
        }

        int followersCount = userRepository.countFollowers(userId);
        int followingCount = userRepository.countFollowing(userId);
        int totalActivities = activityRepository.countByUserId(userId);
        Double totalDistance = activityRepository.sumDistanceByUserId(userId);
        Long totalDuration = activityRepository.sumDurationByUserId(userId);
        Long totalElevation = activityRepository.sumElevationByUserId(userId);
        Long totalCalories = activityRepository.sumCaloriesByUserId(userId);

        List<Activity> recentPublic = activityRepository.findRecentPublicByUserId(userId, PageRequest.of(0, 5));
        List<ActivityResponse> recentActivities = recentPublic.stream()
                .map(activity -> {
                    ActivityResponse response = commonMapper.convertToResponse(activity, ActivityResponse.class);
                    response.setLikesCount(activity.getLikesCount());
                    response.setCommentsCount(activity.getCommentsCount());
                    if (currentUserId != null) {
                        response.setLikedByCurrentUser(activityRepository.isLikedByUser(activity.getId(), currentUserId));
                    }
                    return response;
                })
                .collect(Collectors.toList());

        return UserProfileResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .bio(user.getBio())
                .city(user.getCity())
                .country(user.getCountry())
                .avatar(user.getAvatar())
                .createdAt(user.getCreatedAt())
                .totalActivities(totalActivities)
                .totalDistance(totalDistance != null ? totalDistance : 0.0)
                .totalDurationSeconds(totalDuration != null ? totalDuration : 0L)
                .totalElevationMeters(totalElevation != null ? totalElevation : 0L)
                .totalCalories(totalCalories != null ? totalCalories : 0L)
                .followersCount(followersCount)
                .followingCount(followingCount)
                .recentActivities(recentActivities)
                .isFollowedByCurrentUser(isFollower)
                .isPrivate(isPrivate)
                .canViewActivities(true)
                .build();
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new ApiRequestException(PASSWORD_CHANGE_NOT_ALLOWED, HttpStatus.BAD_REQUEST);
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ApiRequestException(INCORRECT_PASSWORD, HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));

        userRepository.delete(user);
        eventPublisher.publishEvent(new UserDeletedEvent(userId));
    }
}
