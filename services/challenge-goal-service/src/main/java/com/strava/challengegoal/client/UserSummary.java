package com.strava.challengegoal.client;

/**
 * Shape of one element returned by user-service's internal batch endpoint
 * {@code GET /internal/users?ids=1,2,3}.
 */
public record UserSummary(
        Long id,
        String firstName,
        String lastName,
        String fullName,
        String avatar
) {

    public String displayName() {
        if (fullName != null && !fullName.isBlank()) return fullName;
        if (firstName != null || lastName != null) {
            return ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
        }
        return "User #" + id;
    }
}
