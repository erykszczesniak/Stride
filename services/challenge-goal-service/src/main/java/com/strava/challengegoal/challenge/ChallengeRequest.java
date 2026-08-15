package com.strava.challengegoal.challenge;

import com.strava.challengegoal.common.ActivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Verbatim copy of the monolith's ChallengeRequest — field names and Jackson
 * behavior (e.g. the boolean binding as "public") must match exactly.
 */
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
