package com.strava.backend.service;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.PersonalRecord;
import com.strava.backend.enums.ActivityType;

import java.util.List;

public interface PersonalRecordService {

    List<PersonalRecord> checkAndUpdatePRs(Activity activity);

    List<PersonalRecord> getUserPRs(Long userId);

    List<PersonalRecord> getUserPRsByActivityType(Long userId, ActivityType activityType);

    void recalculateAllPRs(Long userId);
}
