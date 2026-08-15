package com.strava.activityservice.admin;

import com.strava.activityservice.domain.ActivityType;
import com.strava.activityservice.user.BaseUserResponse;
import lombok.Data;

import java.time.LocalDateTime;

/** Ported 1:1 from main. */
@Data
public class AdminActivityResponse {
    private Long id;
    private String title;
    private ActivityType activityType;
    private Double distance;
    private Integer duration;
    private Integer calories;
    private LocalDateTime startTime;
    private LocalDateTime createdAt;
    private BaseUserResponse user;
}
