package com.strava.backend.dto.challenge;

import com.strava.backend.enums.ActivityType;
import com.strava.backend.enums.ChallengeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChallengeRequest {

    @NotBlank(message = "Challenge name is required")
    private String name;

    private String description;
    private String coverImage;

    @NotNull(message = "Challenge type is required")
    private ChallengeType challengeType;

    private ActivityType activityType;

    @NotNull(message = "Target value is required")
    @Positive(message = "Target value must be positive")
    private Double targetValue;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;

    private boolean isPublic;
}
