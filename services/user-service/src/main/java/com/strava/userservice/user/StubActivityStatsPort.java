package com.strava.userservice.user;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Placeholder implementation until activity-service exposes its batch stats API
 * (parity plan Phase 13); profiles render with zero activity stats and an empty
 * recent-activities list until then.
 */
@Component
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
