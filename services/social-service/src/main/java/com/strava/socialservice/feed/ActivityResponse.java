package com.strava.socialservice.feed;

import com.strava.socialservice.user.BaseUserResponse;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Ported 1:1 from main — field names and JSON shape are the public contract.
 * Note the two deliberate serialization quirks inherited from main:
 *   * the Lombok getter for {@code isPrivate} is {@code isPrivate()}, which
 *     Jackson maps to the JSON key {@code "private"};
 *   * {@code BaseUserResponse.getFullName()} adds a derived "fullName" key.
 */
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
    private String routePolyline;
    // Per-point altitudes in meters, aligned with the polyline. Null when the
    // activity has no stored elevation series (e.g. legacy rows or non-GPX sources).
    private double[] elevationProfile;
    // Per-point epoch-seconds timestamps, aligned 1:1 with the polyline points.
    // Null for legacy activities and imports whose source carried no per-point time
    // data — the frontend falls back to uniform-pace splits when this is null.
    private long[] pointTimestamps;
    private Integer averageHeartRate;
    private Integer maxHeartRate;
    private List<String> photos;
    private ActivityGearSummary gear;
}
