package com.strava.challengegoal.challenge;

import com.strava.challengegoal.common.ActivityType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Verbatim copy of the monolith's ChallengeResponse. The Lombok "isX" boolean
 * fields intentionally serialize as "participating"/"active"/"public" — the
 * frontend depends on those exact keys.
 */
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
    private Long creatorId;
}
