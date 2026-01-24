package com.strava.backend.dto.activity;

import com.strava.backend.dto.user.BaseUserResponse;
import com.strava.backend.enums.ActivityType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityResponse {
    private Long id;
    private String title;
    private String description;
    private ActivityType activityType;
    private Double distance;
    private Integer duration;
    private Integer elevationGain;
    private Integer calories;
    private Double averageSpeed;
    private LocalDateTime startTime;
    private String startLocation;
    private boolean isPrivate;
    private int likesCount;
    private int commentsCount;
    private boolean likedByCurrentUser;
    private LocalDateTime createdAt;
    private BaseUserResponse user;
}
