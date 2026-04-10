package com.strava.backend.dto.personalrecord;

import com.strava.backend.enums.ActivityType;
import com.strava.backend.enums.DistanceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalRecordResponse {
    private Long id;
    private ActivityType activityType;
    private DistanceType distanceType;
    private String distanceDisplayName;
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
