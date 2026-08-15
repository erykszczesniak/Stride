package com.strava.userservice.user;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Public profile payload, mirrored from the monolith's {@code UserProfileResponse}.
 *
 * <p>JSON key parity: the monolith class uses Lombok on primitive booleans, so Jackson
 * emits {@code followedByCurrentUser} (from {@code isFollowedByCurrentUser()}) and
 * {@code private} (from {@code isPrivate()}) — the record components reproduce exactly
 * those keys.
 *
 * <p>{@code recentActivities} is typed loosely because activity data lives in
 * activity-service; see {@link ActivityStatsPort}.
 */
public record UserProfileResponse(
        Long id,
        String firstName,
        String lastName,
        String bio,
        String city,
        String country,
        String avatar,
        LocalDateTime createdAt,
        int totalActivities,
        double totalDistance,
        long totalDurationSeconds,
        long totalElevationMeters,
        long totalCalories,
        int followersCount,
        int followingCount,
        List<Object> recentActivities,
        boolean followedByCurrentUser,
        @JsonProperty("private") boolean isPrivate,
        boolean canViewActivities
) {
    /**
     * Sanitized variant returned for private profiles when the viewer is neither the
     * owner nor a follower — name + avatar only, mirroring the monolith's short-circuit.
     */
    public static UserProfileResponse sanitized(Long id, String firstName, String lastName, String avatar) {
        return new UserProfileResponse(
                id, firstName, lastName, null, null, null, avatar, null,
                0, 0.0, 0L, 0L, 0L, 0, 0,
                List.of(), false, true, false
        );
    }
}
