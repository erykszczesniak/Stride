package com.strava.activityservice.util;

import com.strava.activityservice.activity.ActivityRequest;
import com.strava.activityservice.domain.Activity;
import com.strava.activityservice.domain.ActivityType;

import java.time.LocalDateTime;

/** Test fixtures mirroring main's TestDataFactory (activity subset). */
public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static Activity createActivity(Long id, Long userId) {
        return Activity.builder()
                .id(id)
                .userId(userId)
                .title("Morning Run")
                .description("Easy morning run")
                .activityType(ActivityType.RUN)
                .distance(10.0)
                .duration(3600)
                .startTime(LocalDateTime.now().minusHours(2))
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();
    }

    public static ActivityRequest createActivityRequest() {
        ActivityRequest request = new ActivityRequest();
        request.setTitle("Evening Ride");
        request.setDescription("Nice ride along the river");
        request.setActivityType(ActivityType.RIDE);
        request.setDistance(25.0);
        request.setDuration(4500);
        request.setElevationGain(120);
        request.setCalories(600);
        request.setStartTime(LocalDateTime.now().minusHours(3));
        return request;
    }
}
