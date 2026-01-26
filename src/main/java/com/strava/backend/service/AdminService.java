package com.strava.backend.service;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.User;
import com.strava.backend.dto.admin.AdminStatsResponse;
import com.strava.backend.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminService {

    AdminStatsResponse getAdminStats();

    Page<User> getAllUsers(Pageable pageable);

    User getUserById(Long userId);

    void updateUserRole(Long userId, Role role);

    void toggleUserActive(Long userId);

    void deleteUser(Long userId);

    Page<Activity> getAllActivities(Pageable pageable);

    Page<Activity> searchActivities(String search, Pageable pageable);

    void deleteActivity(Long activityId);
}
