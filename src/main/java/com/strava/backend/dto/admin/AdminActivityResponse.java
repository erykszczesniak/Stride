package com.strava.backend.dto.admin;

import com.strava.backend.dto.user.BaseUserResponse;
import com.strava.backend.enums.ActivityType;
import lombok.Data;

import java.time.LocalDateTime;

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
