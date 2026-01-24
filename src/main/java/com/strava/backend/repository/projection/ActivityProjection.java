package com.strava.backend.repository.projection;

import com.strava.backend.enums.ActivityType;

import java.time.LocalDateTime;

public interface ActivityProjection {
    Long getId();
    String getTitle();
    String getDescription();
    ActivityType getActivityType();
    Double getDistance();
    Integer getDuration();
    Integer getElevationGain();
    Integer getCalories();
    Double getAverageSpeed();
    LocalDateTime getStartTime();
    String getStartLocation();
    boolean getIsPrivate();
    LocalDateTime getCreatedAt();

    UserProjection getUser();

    interface UserProjection {
        Long getId();
        String getFirstName();
        String getLastName();
        String getAvatar();
    }
}
