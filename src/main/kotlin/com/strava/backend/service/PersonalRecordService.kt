package com.strava.backend.service

import com.strava.backend.domain.Activity
import com.strava.backend.domain.PersonalRecord
import com.strava.backend.enums.ActivityType

interface PersonalRecordService {

    fun checkAndUpdatePRs(activity: Activity): List<PersonalRecord>

    fun getUserPRs(userId: Long): List<PersonalRecord>

    /**
     * Returns PRs for [targetUserId], respecting the target user's privacy settings.
     * Returns an empty list if the target user has `isPrivate=true` and
     * [viewerId] is neither the target user themselves nor a follower.
     */
    fun getUserPRsVisibleTo(targetUserId: Long, viewerId: Long?): List<PersonalRecord>

    fun getUserPRsByActivityType(userId: Long, activityType: ActivityType): List<PersonalRecord>

    fun recalculateAllPRs(userId: Long)
}
