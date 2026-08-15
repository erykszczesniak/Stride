package com.strava.userservice.user;

import com.strava.userservice.domain.AuthProvider;
import com.strava.userservice.domain.User;
import com.strava.userservice.domain.UserFollow;
import com.strava.userservice.domain.UserFollowRepository;
import com.strava.userservice.domain.UserRepository;
import com.strava.userservice.event.UserEventPublisher;
import com.strava.userservice.exception.ApiRequestException;
import com.strava.userservice.search.UserDeletedEvent;
import com.strava.userservice.search.UserDocument;
import com.strava.userservice.search.UserSearchRepository;
import com.strava.userservice.search.UserUpsertedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.strava.userservice.common.ErrorMessages.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserFollowRepository followRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher localEvents;
    private final UserEventPublisher kafkaEvents;
    private final ActivityStatsPort activityStats;
    /**
     * Provider rather than a direct dependency: the Elasticsearch repository does not
     * exist in the {@code standalone} profile (H2, no ES) — search then falls back to SQL.
     */
    private final ObjectProvider<UserSearchRepository> searchRepositoryProvider;

    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserResponse(Long userId) {
        User user = getUserById(userId);
        return UserResponse.from(user,
                (int) followRepository.countByUserId(userId),
                (int) followRepository.countByFollowerId(userId),
                activityStats.countActivities(userId));
    }

    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * Elasticsearch-backed full-text search. Throws when ES is unavailable so the
     * controller can fall back to {@link #searchUsersFallback(String, Pageable)} —
     * same layering as the monolith.
     */
    @Transactional(readOnly = true)
    public Page<User> searchUsers(String searchText, Pageable pageable) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return userRepository.findAll(pageable);
        }
        UserSearchRepository searchRepository = searchRepositoryProvider.getIfAvailable();
        if (searchRepository == null) {
            throw new IllegalStateException("Elasticsearch search is not available in this profile");
        }
        Page<UserDocument> hits = searchRepository.searchByQuery(searchText.trim(), pageable);
        List<Long> ids = hits.getContent().stream().map(UserDocument::getId).toList();
        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, hits.getTotalElements());
        }
        // Preserve the Elasticsearch-scored order when hydrating from JPA.
        Map<Long, User> byId = new HashMap<>();
        for (User u : userRepository.findByIdIn(ids)) {
            byId.put(u.getId(), u);
        }
        List<User> ordered = ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();
        return new PageImpl<>(ordered, pageable, hits.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<User> searchUsersFallback(String searchText, Pageable pageable) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return userRepository.findAll(pageable);
        }
        return userRepository.searchUsersByText(searchText.trim(), pageable);
    }

    @Transactional
    public User updateUser(Long userId, UpdateUserRequest request) {
        User user = getUserById(userId);
        // Parity with the monolith: name/bio/location/measurements overwrite unconditionally
        // (null clears the field), the toggles only when present in the payload.
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setBio(request.bio());
        user.setCity(request.city());
        user.setCountry(request.country());
        user.setWeight(request.weight());
        user.setHeight(request.height());
        if (request.emailNotifications() != null) {
            user.setEmailNotifications(request.emailNotifications());
        }
        if (request.pushNotifications() != null) {
            user.setPushNotifications(request.pushNotifications());
        }
        if (request.weeklySummary() != null) {
            user.setWeeklySummary(request.weeklySummary());
        }
        if (request.isPrivate() != null) {
            user.setIsPrivate(request.isPrivate());
        }
        if (request.hideFromSearch() != null) {
            user.setHideFromSearch(request.hideFromSearch());
        }
        User saved = userRepository.save(user);
        localEvents.publishEvent(new UserUpsertedEvent(saved));
        kafkaEvents.publishUserUpdated(saved);
        return saved;
    }

    @Transactional
    public void followUser(Long userId, Long followUserId) {
        User user = getUserById(userId);
        getUserById(followUserId);

        if (userId.equals(followUserId)) {
            throw new ApiRequestException("Cannot follow yourself", HttpStatus.BAD_REQUEST);
        }
        if (followRepository.existsByUserIdAndFollowerId(followUserId, userId)) {
            throw new ApiRequestException("Already following this user", HttpStatus.BAD_REQUEST);
        }

        followRepository.save(UserFollow.builder()
                .userId(followUserId)
                .followerId(userId)
                .build());

        // The FOLLOW notification the monolith created in-process is now produced by
        // notification-service from this event (hence followerDisplayName on it).
        kafkaEvents.publishUserFollowed(user, followUserId);
    }

    @Transactional
    public void unfollowUser(Long userId, Long unfollowUserId) {
        getUserById(userId);
        getUserById(unfollowUserId);

        if (!followRepository.existsByUserIdAndFollowerId(unfollowUserId, userId)) {
            throw new ApiRequestException("Not following this user", HttpStatus.BAD_REQUEST);
        }

        followRepository.deleteByUserIdAndFollowerId(unfollowUserId, userId);
        kafkaEvents.publishUserUnfollowed(userId, unfollowUserId);
    }

    @Transactional(readOnly = true)
    public List<User> getFollowers(Long userId) {
        getUserById(userId);
        List<Long> followerIds = followRepository.findAllByUserId(userId).stream()
                .map(UserFollow::getFollowerId)
                .toList();
        return followerIds.isEmpty() ? List.of() : userRepository.findByIdIn(followerIds);
    }

    @Transactional(readOnly = true)
    public List<User> getFollowing(Long userId) {
        getUserById(userId);
        List<Long> followedIds = followRepository.findAllByFollowerId(userId).stream()
                .map(UserFollow::getUserId)
                .toList();
        return followedIds.isEmpty() ? List.of() : userRepository.findByIdIn(followedIds);
    }

    @Transactional(readOnly = true)
    public List<User> getSuggestedUsers(Long userId, int limit) {
        return userRepository.findSuggestedUsers(userId, PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId, Long currentUserId) {
        User user = getUserById(userId);

        boolean isPrivate = Boolean.TRUE.equals(user.getIsPrivate());
        boolean isSelf = currentUserId != null && currentUserId.equals(userId);
        boolean isFollower = currentUserId != null && !isSelf
                && followRepository.existsByUserIdAndFollowerId(userId, currentUserId);
        boolean canViewActivities = !isPrivate || isSelf || isFollower;

        // Privacy short-circuit: private profile + viewer is neither owner nor follower
        // -> sanitized payload (name + avatar only), mirroring the monolith.
        if (!canViewActivities) {
            return UserProfileResponse.sanitized(
                    user.getId(), user.getFirstName(), user.getLastName(), user.getAvatar());
        }

        ActivityStatsPort.ActivityTotals totals = activityStats.totalsFor(userId);
        return new UserProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getBio(),
                user.getCity(),
                user.getCountry(),
                user.getAvatar(),
                user.getCreatedAt(),
                totals.totalActivities(),
                totals.totalDistance(),
                totals.totalDurationSeconds(),
                totals.totalElevationMeters(),
                totals.totalCalories(),
                (int) followRepository.countByUserId(userId),
                (int) followRepository.countByFollowerId(userId),
                activityStats.recentPublicActivities(userId, currentUserId),
                isFollower,
                isPrivate,
                true
        );
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getUserById(userId);

        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new ApiRequestException(PASSWORD_CHANGE_NOT_ALLOWED, HttpStatus.BAD_REQUEST);
        }
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ApiRequestException(INCORRECT_PASSWORD, HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void deleteAccount(Long userId) {
        User user = getUserById(userId);
        userRepository.delete(user);
        localEvents.publishEvent(new UserDeletedEvent(userId));
        kafkaEvents.publishUserDeleted(userId);
    }
}
