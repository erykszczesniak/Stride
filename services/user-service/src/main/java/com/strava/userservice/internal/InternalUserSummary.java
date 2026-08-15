package com.strava.userservice.internal;

import com.strava.userservice.domain.User;

/** Cross-service user card — the batch lookup shape other services cache/hydrate from. */
public record InternalUserSummary(
        Long id,
        String firstName,
        String lastName,
        String fullName,
        String avatar
) {
    public static InternalUserSummary from(User user) {
        return new InternalUserSummary(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getFullName(),
                user.getAvatar()
        );
    }
}
