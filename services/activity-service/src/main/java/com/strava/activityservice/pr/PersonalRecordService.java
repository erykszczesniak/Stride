package com.strava.activityservice.pr;

import com.strava.activityservice.client.UserVisibilityClient;
import com.strava.activityservice.domain.*;
import com.strava.activityservice.event.ActivityEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Personal record computation, aligned 1:1 with main's Kotlin
 * PersonalRecordServiceImpl.
 *
 * A single activity registers at most ONE distance type — the largest
 * applicable milestone that fits within the activity's total distance.
 * We only have average pace across the whole activity (no per-segment
 * timing), so claiming a 50 m PR off a 1 km cruise-pace swim would say
 * "you swam 50 m in 54 s" — but you didn't; you averaged that over a long
 * swim. Only the largest distance the activity actually covered is honest.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalRecordService {

    /**
     * Per-sport PR distance matrix (mirrors main):
     *   - Running / walking / hiking: 1, 5, 10 km, Half Marathon, Marathon
     *   - Cycling: 1, 5, 10 km
     *   - Swimming: 50/100/200/400 m (pool sprints) + 1 km, 3 km (open water)
     *   - Other distance-tracked sports (rowing, kayak, skiing, skating):
     *     the generic 1/5/10 km set
     * Sports not in this map (WORKOUT, YOGA, WEIGHT_TRAINING, ...) get no PRs
     * at all — distance is meaningless (or misleading) there.
     */
    private static final Map<ActivityType, Set<DistanceType>> DISTANCES_BY_SPORT = buildDistanceMatrix();

    private final PersonalRecordRepository personalRecordRepository;
    private final ActivityRepository activityRepository;
    private final UserVisibilityClient userVisibilityClient;
    private final ActivityEventPublisher eventPublisher;

    @Transactional
    public List<PersonalRecord> checkAndUpdatePRs(Activity activity) {
        Double distance = activity.getDistance();
        Integer duration = activity.getDuration();
        if (distance == null || duration == null || distance <= 0 || duration <= 0) {
            return List.of();
        }

        if (!DISTANCES_BY_SPORT.containsKey(activity.getActivityType())) {
            return List.of();
        }

        Map<DistanceType, Integer> bestTimes = findBestTimesForActivity(activity);
        List<PersonalRecord> updatedPRs = new ArrayList<>();

        for (Map.Entry<DistanceType, Integer> entry : bestTimes.entrySet()) {
            DistanceType distanceType = entry.getKey();
            int timeInSeconds = entry.getValue();

            Optional<PersonalRecord> existingPR = personalRecordRepository.findByUserIdAndActivityTypeAndDistanceType(
                    activity.getUserId(), activity.getActivityType(), distanceType);

            if (existingPR.isPresent()) {
                PersonalRecord pr = existingPR.get();
                if (timeInSeconds < pr.getTimeInSeconds()) {
                    pr.setPreviousTimeInSeconds(pr.getTimeInSeconds());
                    pr.setTimeInSeconds(timeInSeconds);
                    pr.setActivityId(activity.getId());
                    pr.setAchievedAt(activity.getStartTime());
                    PersonalRecord saved = personalRecordRepository.save(pr);
                    updatedPRs.add(saved);
                    eventPublisher.publishPersonalRecordAchieved(saved);
                    log.info("New PR for user {}: {} {} in {}s (previous: {}s)",
                            activity.getUserId(), activity.getActivityType(),
                            distanceType, timeInSeconds, pr.getPreviousTimeInSeconds());
                }
            } else {
                PersonalRecord pr = PersonalRecord.builder()
                        .userId(activity.getUserId())
                        .activityId(activity.getId())
                        .activityType(activity.getActivityType())
                        .distanceType(distanceType)
                        .timeInSeconds(timeInSeconds)
                        .distance(distanceType.distanceKm)
                        .achievedAt(activity.getStartTime())
                        .build();
                PersonalRecord saved = personalRecordRepository.save(pr);
                updatedPRs.add(saved);
                eventPublisher.publishPersonalRecordAchieved(saved);
                log.info("First PR for user {}: {} {} in {}s",
                        activity.getUserId(), activity.getActivityType(),
                        distanceType, timeInSeconds);
            }
        }

        return updatedPRs;
    }

    @Transactional(readOnly = true)
    public List<PersonalRecord> getUserPRs(Long userId) {
        return personalRecordRepository.findByUserId(userId);
    }

    /**
     * PRs for targetUserId, respecting the target's profile privacy: empty
     * list when the target is private and the viewer is neither the target
     * themselves nor a follower. Privacy facts come from user-service via
     * UserVisibilityClient (fail-open, same tradeoff as ActivityVisibility).
     */
    @Transactional(readOnly = true)
    public List<PersonalRecord> getUserPRsVisibleTo(Long targetUserId, Long viewerId) {
        if (viewerId != null && viewerId.equals(targetUserId)) {
            // Users always see their own PRs, regardless of privacy flag
            return personalRecordRepository.findByUserId(targetUserId);
        }

        UserVisibilityClient.VisibilityInfo target = userVisibilityClient.visibilityFor(targetUserId, viewerId);
        if (target.isPrivate() && !(viewerId != null && target.viewerFollows())) {
            return List.of();
        }

        return personalRecordRepository.findByUserId(targetUserId);
    }

    @Transactional(readOnly = true)
    public List<PersonalRecord> getUserPRsByActivityType(Long userId, ActivityType activityType) {
        return personalRecordRepository.findByUserIdAndActivityType(userId, activityType);
    }

    /**
     * Full rebuild: wipe the user's PRs, then replay every activity in
     * chronological order through checkAndUpdatePRs — reproducing the
     * previous-best chain exactly as it would have accrued.
     */
    @Transactional
    public void recalculateAllPRs(Long userId) {
        personalRecordRepository.deleteByUserId(userId);

        List<Activity> activities = activityRepository.findAllByUserIdOrderByStartTimeAsc(userId);
        for (Activity activity : activities) {
            checkAndUpdatePRs(activity);
        }

        log.info("Recalculated all PRs for user {}", userId);
    }

    /** Cascade used by the activity delete paths (mirrors main's FK cascade). */
    @Transactional
    public void deleteByActivityId(Long activityId) {
        personalRecordRepository.deleteByActivityId(activityId);
    }

    /**
     * Find the PR for a single activity: at most one distance type — the
     * largest applicable milestone that fits within the total distance —
     * timed by scaling total duration to the milestone at average pace.
     */
    private Map<DistanceType, Integer> findBestTimesForActivity(Activity activity) {
        Map<DistanceType, Integer> bestTimes = new EnumMap<>(DistanceType.class);
        double activityDistanceKm = activity.getDistance();

        Set<DistanceType> allowed = DISTANCES_BY_SPORT.get(activity.getActivityType());
        DistanceType largestFit = null;
        for (DistanceType candidate : DistanceType.values()) {
            if (!allowed.contains(candidate)) continue;
            if (activityDistanceKm < candidate.distanceKm) continue;
            if (largestFit == null || candidate.distanceKm > largestFit.distanceKm) {
                largestFit = candidate;
            }
        }

        if (largestFit != null) {
            int estimatedTime = (int) Math.round(
                    (largestFit.distanceKm / activityDistanceKm) * activity.getDuration());
            if (estimatedTime > 0) {
                bestTimes.put(largestFit, estimatedTime);
            }
        }

        return bestTimes;
    }

    private static Map<ActivityType, Set<DistanceType>> buildDistanceMatrix() {
        Set<DistanceType> runningSet = EnumSet.of(
                DistanceType.ONE_KM, DistanceType.FIVE_KM, DistanceType.TEN_KM,
                DistanceType.HALF_MARATHON, DistanceType.MARATHON);
        Set<DistanceType> cyclingSet = EnumSet.of(
                DistanceType.ONE_KM, DistanceType.FIVE_KM, DistanceType.TEN_KM);
        Set<DistanceType> swimmingSet = EnumSet.of(
                DistanceType.FIFTY_M, DistanceType.HUNDRED_M, DistanceType.TWO_HUNDRED_M,
                DistanceType.FOUR_HUNDRED_M, DistanceType.ONE_KM, DistanceType.THREE_KM);
        Set<DistanceType> genericSet = EnumSet.of(
                DistanceType.ONE_KM, DistanceType.FIVE_KM, DistanceType.TEN_KM);

        Map<ActivityType, Set<DistanceType>> matrix = new EnumMap<>(ActivityType.class);
        matrix.put(ActivityType.RUN, runningSet);
        matrix.put(ActivityType.VIRTUAL_RUN, runningSet);
        matrix.put(ActivityType.WALK, runningSet);
        matrix.put(ActivityType.HIKE, runningSet);
        matrix.put(ActivityType.RIDE, cyclingSet);
        matrix.put(ActivityType.VIRTUAL_RIDE, cyclingSet);
        matrix.put(ActivityType.SWIM, swimmingSet);
        matrix.put(ActivityType.ROWING, genericSet);
        matrix.put(ActivityType.KAYAKING, genericSet);
        matrix.put(ActivityType.STAND_UP_PADDLING, genericSet);
        matrix.put(ActivityType.SURFING, genericSet);
        matrix.put(ActivityType.NORDIC_SKI, genericSet);
        matrix.put(ActivityType.ALPINE_SKI, genericSet);
        matrix.put(ActivityType.BACKCOUNTRY_SKI, genericSet);
        matrix.put(ActivityType.ROLLER_SKI, genericSet);
        matrix.put(ActivityType.ICE_SKATE, genericSet);
        matrix.put(ActivityType.INLINE_SKATE, genericSet);
        matrix.put(ActivityType.SNOWBOARD, genericSet);
        matrix.put(ActivityType.SNOWSHOE, genericSet);
        return matrix;
    }
}
