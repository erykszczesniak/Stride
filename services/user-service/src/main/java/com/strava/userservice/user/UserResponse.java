package com.strava.userservice.user;

import com.strava.userservice.domain.Role;
import com.strava.userservice.domain.User;

import java.time.LocalDateTime;

/**
 * Full user payload, mirrored field-for-field from the monolith's {@code UserResponse}
 * (which extends {@code BaseUserResponse}; records cannot, so the base fields are inlined).
 *
 * <p>Parity note: like the monolith, list/search endpoints and auth responses leave the
 * three counters at 0 — only {@code GET /me} and {@code GET /{userId}} populate them.
 */
public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String avatar,
        String fullName,
        String email,
        String bio,
        String city,
        String country,
        Double weight,
        Double height,
        Role role,
        LocalDateTime createdAt,
        int followersCount,
        int followingCount,
        int activitiesCount,
        boolean emailNotifications,
        boolean pushNotifications,
        boolean weeklySummary,
        Boolean isPrivate,
        Boolean hideFromSearch
) {
    /** Mapping without counters — mirrors the monolith's bare ModelMapper conversion. */
    public static UserResponse from(User user) {
        return from(user, 0, 0, 0);
    }

    public static UserResponse from(User user, int followersCount, int followingCount, int activitiesCount) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getAvatar(),
                user.getFullName(),
                user.getEmail(),
                user.getBio(),
                user.getCity(),
                user.getCountry(),
                user.getWeight(),
                user.getHeight(),
                user.getRole(),
                user.getCreatedAt(),
                followersCount,
                followingCount,
                activitiesCount,
                user.isEmailNotifications(),
                user.isPushNotifications(),
                user.isWeeklySummary(),
                user.getIsPrivate(),
                user.getHideFromSearch()
        );
    }
}
