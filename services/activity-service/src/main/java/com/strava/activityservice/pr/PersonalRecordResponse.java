package com.strava.activityservice.pr;

import com.strava.activityservice.domain.ActivityType;
import com.strava.activityservice.domain.DistanceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Ported 1:1 from main. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalRecordResponse {
    private Long id;
    private ActivityType activityType;
    private DistanceType distanceType;
    private Double distance;
    private Integer timeInSeconds;
    private String formattedTime;
    private String formattedPace;     // min/km
    private Double speedKmh;
    private LocalDateTime achievedAt;
    private Long activityId;
    private String activityTitle;
    private Integer previousTimeInSeconds;
    private Integer improvementSeconds;
    private Double improvementPercent;
}
