package com.strava.backend.service.impl;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.User;
import com.strava.backend.dto.admin.AdminStatsResponse;
import com.strava.backend.enums.Role;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.repository.*;
import com.strava.backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

import static com.strava.backend.constants.ErrorMessage.ACTIVITY_NOT_FOUND;
import static com.strava.backend.constants.ErrorMessage.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final ClubRepository clubRepository;
    private final ChallengeRepository challengeRepository;
    private final EventRepository eventRepository;

    @Override
    public AdminStatsResponse getAdminStats() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        LocalDateTime startOfMonth = LocalDateTime.now().with(TemporalAdjusters.firstDayOfMonth()).toLocalDate().atStartOfDay();

        return AdminStatsResponse.builder()
                .totalUsers(userRepository.count())
                .activeUsers(userRepository.countByActive(true))
                .totalActivities(activityRepository.count())
                .totalClubs(clubRepository.count())
                .totalChallenges(challengeRepository.count())
                .totalEvents(eventRepository.count())
                .newUsersThisWeek(userRepository.countUsersCreatedSince(oneWeekAgo))
                .activitiesThisWeek(activityRepository.countActivitiesCreatedSince(oneWeekAgo))
                .totalDistanceKm(activityRepository.sumAllDistances())
                .activitiesThisMonth(activityRepository.countActivitiesThisMonth(startOfMonth))
                .build();
    }

    @Override
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Override
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    @Override
    @Transactional
    public void updateUserRole(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));
        user.setRole(role);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void toggleUserActive(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));
        user.setActive(!user.isActive());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));
        userRepository.delete(user);
    }

    @Override
    public Page<Activity> getAllActivities(Pageable pageable) {
        return activityRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Override
    public Page<Activity> searchActivities(String search, Pageable pageable) {
        return activityRepository.searchByTitle(search, pageable);
    }

    @Override
    @Transactional
    public void deleteActivity(Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND));
        activityRepository.delete(activity);
    }
}
