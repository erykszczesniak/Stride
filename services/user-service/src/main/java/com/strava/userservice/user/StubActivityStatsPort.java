package com.strava.userservice.user;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Standalone-profile implementation: no discovery, no activity-service, so
 * profiles render with zero activity stats and an empty recent-activities list.
 * Composed environments use {@code ActivityStatsPortAdapter}.
 */
@Component
@Profile("standalone")
public class StubActivityStatsPort implements ActivityStatsPort {

    @Override
    public ActivityTotals totalsFor(Long userId) {
        return ActivityTotals.empty();
    }

    @Override
    public List<Object> recentPublicActivities(Long userId, Long viewerId) {
        return List.of();
    }
}
