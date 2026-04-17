package com.strava.backend.service;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.PersonalRecord;
import com.strava.backend.enums.ActivityType;

import java.util.List;

public interface PersonalRecordService {

    List<PersonalRecord> checkAndUpdatePRs(Activity activity);

    List<PersonalRecord> getUserPRs(Long userId);

    /**
     * Returns PRs for {@code targetUserId}, respecting the target user's privacy settings.
     * Returns an empty list if the target user has {@code isPrivate=true} and
     * {@code viewerId} is neither the target user themselves nor a follower.
     */
    List<PersonalRecord> getUserPRsVisibleTo(Long targetUserId, Long viewerId);

    List<PersonalRecord> getUserPRsByActivityType(Long userId, ActivityType activityType);

    void recalculateAllPRs(Long userId);
}
