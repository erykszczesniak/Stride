package com.strava.backend.dto.activity;

import com.strava.backend.dto.user.BaseUserResponse;
import com.strava.backend.enums.ActivityType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class FullActivityResponse {
    private Long id;
    private String title;
    private String description;
    private ActivityType activityType;
    private Double distance;
    private Integer duration;
    private Integer elevationGain;
    private Integer calories;
    private Double averageSpeed;
    private Double maxSpeed;
    private Integer averageHeartRate;
    private Integer maxHeartRate;
    private Integer averageCadence;
    private Integer averagePower;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String startLocation;
    private String endLocation;
    private String routePolyline;
    // Per-point altitudes in meters, aligned with the polyline. Null when the
    // activity has no stored elevation series (e.g. legacy rows or non-GPX sources).
    private double[] elevationProfile;
    private String mapImageUrl;
    private boolean isPrivate;
    private int likesCount;
    private int commentsCount;
    private boolean likedByCurrentUser;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BaseUserResponse user;
    private List<CommentResponse> comments;
    private List<String> photos;
}
