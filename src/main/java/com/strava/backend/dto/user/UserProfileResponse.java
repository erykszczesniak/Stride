package com.strava.backend.dto.user;

import com.strava.backend.dto.activity.ActivityResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserProfileResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String bio;
    private String city;
    private String country;
    private String avatar;
    private LocalDateTime createdAt;

    private int totalActivities;
    private double totalDistance;
    private int followersCount;
    private int followingCount;

    private List<ActivityResponse> recentActivities;
    private boolean isFollowedByCurrentUser;
}
