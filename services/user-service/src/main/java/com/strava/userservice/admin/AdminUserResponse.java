package com.strava.userservice.admin;

import com.strava.userservice.domain.AuthProvider;
import com.strava.userservice.domain.Role;
import com.strava.userservice.domain.User;

import java.time.LocalDateTime;

/** Admin console user row, mirrored from the monolith's {@code AdminUserResponse}. */
public record AdminUserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String avatar,
        String city,
        String country,
        AuthProvider authProvider,
        Role role,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int activitiesCount,
        int followersCount,
        int followingCount
) {
    public static AdminUserResponse from(User user, int activitiesCount, int followersCount, int followingCount) {
        return new AdminUserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getAvatar(),
                user.getCity(),
                user.getCountry(),
                user.getAuthProvider(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                activitiesCount,
                followersCount,
                followingCount
        );
    }
}
