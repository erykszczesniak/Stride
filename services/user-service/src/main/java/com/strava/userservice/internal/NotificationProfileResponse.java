package com.strava.userservice.internal;

import com.strava.userservice.domain.User;

/** Contact details + notification preferences consumed by notification-service. */
public record NotificationProfileResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String fullName,
        String avatar,
        boolean emailNotifications,
        boolean pushNotifications,
        boolean weeklySummary,
        boolean active
) {
    public static NotificationProfileResponse from(User user) {
        return new NotificationProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getFullName(),
                user.getAvatar(),
                user.isEmailNotifications(),
                user.isPushNotifications(),
                user.isWeeklySummary(),
                user.isActive()
        );
    }
}
