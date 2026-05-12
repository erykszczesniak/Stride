package com.strava.activityservice.pr;

import com.strava.activityservice.domain.ActivityType;
import com.strava.activityservice.domain.DistanceType;
import com.strava.activityservice.domain.PersonalRecord;

import java.time.LocalDateTime;

public record PersonalRecordDto(
        Long id,
        Long userId,
        Long activityId,
        ActivityType activityType,
        DistanceType distanceType,
        Integer timeInSeconds,
        Double distance,
        Integer previousTimeInSeconds,
        LocalDateTime achievedAt
) {
    public static PersonalRecordDto from(PersonalRecord r) {
        return new PersonalRecordDto(
                r.getId(), r.getUserId(), r.getActivityId(),
                r.getActivityType(), r.getDistanceType(),
                r.getTimeInSeconds(), r.getDistance(),
                r.getPreviousTimeInSeconds(), r.getAchievedAt()
        );
    }
}
