package com.strava.userservice.user;

import com.strava.userservice.domain.User;

/**
 * Minimal public user card, mirrored from the monolith's {@code BaseUserResponse}
 * (which computes {@code fullName} via a getter — here it is an explicit component).
 */
public record BaseUserResponse(
        Long id,
        String firstName,
        String lastName,
        String avatar,
        String fullName
) {
    public static BaseUserResponse from(User user) {
        return new BaseUserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getAvatar(),
                user.getFullName()
        );
    }
}
