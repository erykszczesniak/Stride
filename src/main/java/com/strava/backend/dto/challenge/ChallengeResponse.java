package com.strava.backend.dto.challenge;

import com.strava.backend.enums.ActivityType;
import com.strava.backend.enums.ChallengeType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChallengeResponse {
    private Long id;
    private String name;
    private String description;
    private String coverImage;
    private ChallengeType challengeType;
    private ActivityType activityType;
    private Double targetValue;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private int participantsCount;
    private boolean isParticipating;
    private boolean isActive;
    private boolean isPublic;
    private Double userProgress;
}
