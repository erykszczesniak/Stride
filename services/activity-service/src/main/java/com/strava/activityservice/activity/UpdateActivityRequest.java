package com.strava.activityservice.activity;

import com.strava.activityservice.domain.ActivityType;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateActivityRequest(
        @Size(max = 255) String title,
        @Size(max = 2000) String description,
        ActivityType activityType,
        @Positive Double distance,
        @Positive Integer duration,
        Integer elevationGain,
        Integer calories,
        Boolean isPrivate
) {}
