package com.strava.backend.service.impl

import com.strava.backend.domain.Activity
import com.strava.backend.domain.PersonalRecord
import com.strava.backend.enums.ActivityType
import com.strava.backend.enums.DistanceType
import com.strava.backend.repository.ActivityRepository
import com.strava.backend.repository.PersonalRecordRepository
import com.strava.backend.repository.UserRepository
import com.strava.backend.service.PersonalRecordService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.EnumMap
import java.util.EnumSet

@Service
class PersonalRecordServiceImpl(
    private val personalRecordRepository: PersonalRecordRepository,
    private val activityRepository: ActivityRepository,
    private val userRepository: UserRepository?,
) : PersonalRecordService {

    private val log = LoggerFactory.getLogger(PersonalRecordServiceImpl::class.java)

    @Transactional
    override fun checkAndUpdatePRs(activity: Activity): List<PersonalRecord> {
        val distance = activity.distance
        val duration = activity.duration
        if (distance == null || duration == null || distance <= 0 || duration <= 0) {
            return emptyList()
        }

        if (!DISTANCES_BY_SPORT.containsKey(activity.activityType)) {
            return emptyList()
        }

        val bestTimes = findBestTimesForActivity(activity)
        val updatedPRs = mutableListOf<PersonalRecord>()

        for ((distanceType, timeInSeconds) in bestTimes) {
            val existingPR = personalRecordRepository.findByUserIdAndActivityTypeAndDistanceType(
                activity.user.id, activity.activityType, distanceType
            )

            if (existingPR.isPresent) {
                val pr = existingPR.get()
                if (timeInSeconds < pr.timeInSeconds) {
                    pr.previousTimeInSeconds = pr.timeInSeconds
                    pr.timeInSeconds = timeInSeconds
                    pr.activity = activity
                    pr.achievedAt = activity.startTime
                    val saved = personalRecordRepository.save(pr)
                    updatedPRs.add(saved)
                    log.info(
                        "New PR for user {}: {} {} in {}s (previous: {}s)",
                        activity.user.id, activity.activityType,
                        distanceType, timeInSeconds, pr.previousTimeInSeconds
                    )
                }
            } else {
                val pr = PersonalRecord.builder()
                    .user(activity.user)
                    .activity(activity)
                    .activityType(activity.activityType)
                    .distanceType(distanceType)
                    .timeInSeconds(timeInSeconds)
                    .distance(distanceType.distanceKm)
                    .achievedAt(activity.startTime)
                    .build()
                val saved = personalRecordRepository.save(pr)
                updatedPRs.add(saved)
                log.info(
                    "First PR for user {}: {} {} in {}s",
                    activity.user.id, activity.activityType,
                    distanceType, timeInSeconds
                )
            }
        }

        return updatedPRs
    }

    @Transactional(readOnly = true)
    override fun getUserPRs(userId: Long): List<PersonalRecord> =
        personalRecordRepository.findByUserId(userId)

    @Transactional(readOnly = true)
    override fun getUserPRsVisibleTo(targetUserId: Long, viewerId: Long?): List<PersonalRecord> {
        if (viewerId != null && viewerId == targetUserId) {
            // Users always see their own PRs, regardless of privacy flag
            return personalRecordRepository.findByUserId(targetUserId)
        }

        // Non-null in production (Spring injects it); nullable in the type
        // signature only to let mock-based unit tests construct the service
        // without a UserRepository stub when they don't exercise this path.
        val users = userRepository!!
        val targetIsPrivate = users.findById(targetUserId)
            .map { it.isPrivate == true }
            .orElse(false)

        if (targetIsPrivate) {
            val viewerIsFollower = viewerId != null &&
                users.isFollowing(targetUserId, viewerId)
            if (!viewerIsFollower) {
                return emptyList()
            }
        }

        return personalRecordRepository.findByUserId(targetUserId)
    }

    @Transactional(readOnly = true)
    override fun getUserPRsByActivityType(userId: Long, activityType: ActivityType): List<PersonalRecord> =
        personalRecordRepository.findByUserIdAndActivityType(userId, activityType)

    @Transactional
    override fun recalculateAllPRs(userId: Long) {
        personalRecordRepository.deleteByUserId(userId)

        val activities = activityRepository.findAllByUserIdOrderByStartTimeAsc(userId)
        for (activity in activities) {
            checkAndUpdatePRs(activity)
        }

        log.info("Recalculated all PRs for user {}", userId)
    }

    /**
     * Find the PR for a single activity. Returns at most ONE distance type —
     * the largest applicable milestone that fits within the activity's total
     * distance.
     *
     * Rationale: we don't have per-segment timestamps on the polyline, so we
     * can only compute average pace across the whole activity. Claiming a
     * 50 m PR off a 1 km swim done at 1:47 /100m cruise pace would say
     * "you swam 50 m in 54 s" — but you didn't, you just averaged that over
     * a long swim. The same argument applies to running: a 10 km steady run
     * doesn't prove you can run a 1 km PR at that pace (you likely could,
     * but you didn't do a 1 km time trial).
     *
     * Honest behaviour: only register the PR for the largest distance the
     * activity actually covered. A 1 km swim sets a 1 km PR. A 3.3 km swim
     * sets a 3 km PR. A 100 m sprint sets a 100 m PR. Users who want a
     * short-distance PR need to do a short-distance activity.
     *
     * This also kills the old bug where every swim PR showed exactly the
     * same /100m pace because all rows were scaled from the same activity.
     */
    private fun findBestTimesForActivity(activity: Activity): Map<DistanceType, Int> {
        val bestTimes = EnumMap<DistanceType, Int>(DistanceType::class.java)
        val activityDistanceKm = activity.distance

        val largestFit = DistanceType.values()
            .filter { isDistanceApplicable(it, activity.activityType) }
            .filter { activityDistanceKm >= it.distanceKm }
            .maxByOrNull { it.distanceKm }

        largestFit?.let {
            val estimatedTime = Math.round(
                (it.distanceKm / activityDistanceKm) * activity.duration
            ).toInt()
            if (estimatedTime > 0) {
                bestTimes[it] = estimatedTime
            }
        }

        return bestTimes
    }

    /**
     * Whether a given distance type should be tracked for the given sport.
     * Reads the per-sport [DISTANCES_BY_SPORT] map — e.g. Swimming gets
     * 50m/100m/200m/400m/1km/3km, Cycling gets 1/5/10 km only, etc.
     */
    private fun isDistanceApplicable(distanceType: DistanceType, activityType: ActivityType): Boolean {
        val allowed = DISTANCES_BY_SPORT[activityType]
        return allowed != null && allowed.contains(distanceType)
    }

    companion object {
        /**
         * Per-sport PR distance matrix. Each sport tracks the distances that
         * are meaningful milestones for it:
         *
         *   - Running / walking / hiking: 1, 5, 10, Half Marathon, Marathon
         *   - Cycling: 1, 5, 10 km (longer cycling thresholds like 40k or
         *     century would be a separate feature)
         *   - Swimming: 50, 100, 200, 400 m (pool sprints) + 1 km, 3 km
         *     (long-distance open-water)
         *   - Other distance-tracked sports (rowing, kayak, skiing, skating):
         *     keep the generic 1/5/10 km until someone needs something
         *     sport-specific
         *
         * Sports not in this map — WORKOUT, YOGA, WEIGHT_TRAINING, CROSSFIT,
         * ROCK_CLIMBING, ELLIPTICAL, STAIR_STEPPER, etc. — get no PRs at all,
         * because distance is meaningless (or misleading) there.
         */
        private val DISTANCES_BY_SPORT: Map<ActivityType, Set<DistanceType>> = run {
            val runningSet = EnumSet.of(
                DistanceType.ONE_KM, DistanceType.FIVE_KM, DistanceType.TEN_KM,
                DistanceType.HALF_MARATHON, DistanceType.MARATHON
            )
            val cyclingSet = EnumSet.of(
                DistanceType.ONE_KM, DistanceType.FIVE_KM, DistanceType.TEN_KM
            )
            val swimmingSet = EnumSet.of(
                DistanceType.FIFTY_M, DistanceType.HUNDRED_M, DistanceType.TWO_HUNDRED_M,
                DistanceType.FOUR_HUNDRED_M, DistanceType.ONE_KM, DistanceType.THREE_KM
            )
            val genericSet = EnumSet.of(
                DistanceType.ONE_KM, DistanceType.FIVE_KM, DistanceType.TEN_KM
            )

            EnumMap<ActivityType, Set<DistanceType>>(ActivityType::class.java).apply {
                put(ActivityType.RUN, runningSet)
                put(ActivityType.VIRTUAL_RUN, runningSet)
                put(ActivityType.WALK, runningSet)
                put(ActivityType.HIKE, runningSet)
                put(ActivityType.RIDE, cyclingSet)
                put(ActivityType.VIRTUAL_RIDE, cyclingSet)
                put(ActivityType.SWIM, swimmingSet)
                put(ActivityType.ROWING, genericSet)
                put(ActivityType.KAYAKING, genericSet)
                put(ActivityType.STAND_UP_PADDLING, genericSet)
                put(ActivityType.SURFING, genericSet)
                put(ActivityType.NORDIC_SKI, genericSet)
                put(ActivityType.ALPINE_SKI, genericSet)
                put(ActivityType.BACKCOUNTRY_SKI, genericSet)
                put(ActivityType.ROLLER_SKI, genericSet)
                put(ActivityType.ICE_SKATE, genericSet)
                put(ActivityType.INLINE_SKATE, genericSet)
                put(ActivityType.SNOWBOARD, genericSet)
                put(ActivityType.SNOWSHOE, genericSet)
            }
        }
    }
}
