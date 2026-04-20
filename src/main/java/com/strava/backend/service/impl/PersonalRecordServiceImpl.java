package com.strava.backend.service.impl;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.PersonalRecord;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.enums.DistanceType;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.repository.PersonalRecordRepository;
import com.strava.backend.repository.UserRepository;
import com.strava.backend.service.PersonalRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalRecordServiceImpl implements PersonalRecordService {

    /**
     * Activity types that track "race" distances meaningfully.
     * Gym / indoor / climbing-style activities are excluded — a 1 km PR
     * for a WORKOUT makes no sense.
     */
    private static final Set<ActivityType> DISTANCE_TRACKED = EnumSet.of(
            ActivityType.RUN, ActivityType.VIRTUAL_RUN, ActivityType.WALK, ActivityType.HIKE,
            ActivityType.RIDE, ActivityType.VIRTUAL_RIDE,
            ActivityType.SWIM,
            ActivityType.ROWING, ActivityType.KAYAKING, ActivityType.STAND_UP_PADDLING, ActivityType.SURFING,
            ActivityType.NORDIC_SKI, ActivityType.ALPINE_SKI, ActivityType.BACKCOUNTRY_SKI, ActivityType.ROLLER_SKI,
            ActivityType.ICE_SKATE, ActivityType.INLINE_SKATE, ActivityType.SNOWBOARD, ActivityType.SNOWSHOE
    );

    /**
     * Activity types eligible for Half Marathon / Marathon PRs.
     * 21 km / 42 km are running milestones — a cyclist doing 21 km isn't a
     * "half marathon" in any meaningful sense, so restrict those distances
     * to on-foot sports only.
     */
    private static final Set<ActivityType> LONG_RACE_ELIGIBLE = EnumSet.of(
            ActivityType.RUN, ActivityType.VIRTUAL_RUN, ActivityType.WALK, ActivityType.HIKE
    );

    private final PersonalRecordRepository personalRecordRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public List<PersonalRecord> checkAndUpdatePRs(Activity activity) {
        if (activity.getDistance() == null || activity.getDuration() == null
                || activity.getDistance() <= 0 || activity.getDuration() <= 0) {
            return Collections.emptyList();
        }

        if (!DISTANCE_TRACKED.contains(activity.getActivityType())) {
            return Collections.emptyList();
        }

        Map<DistanceType, Integer> bestTimes = findBestTimesForActivity(activity);
        List<PersonalRecord> updatedPRs = new ArrayList<>();

        for (Map.Entry<DistanceType, Integer> entry : bestTimes.entrySet()) {
            DistanceType distanceType = entry.getKey();
            int timeInSeconds = entry.getValue();

            Optional<PersonalRecord> existingPR = personalRecordRepository
                    .findByUserIdAndActivityTypeAndDistanceType(
                            activity.getUser().getId(), activity.getActivityType(), distanceType);

            if (existingPR.isPresent()) {
                PersonalRecord pr = existingPR.get();
                if (timeInSeconds < pr.getTimeInSeconds()) {
                    pr.setPreviousTimeInSeconds(pr.getTimeInSeconds());
                    pr.setTimeInSeconds(timeInSeconds);
                    pr.setActivity(activity);
                    pr.setAchievedAt(activity.getStartTime());
                    PersonalRecord saved = personalRecordRepository.save(pr);
                    updatedPRs.add(saved);
                    log.info("New PR for user {}: {} {} in {}s (previous: {}s)",
                            activity.getUser().getId(), activity.getActivityType(),
                            distanceType, timeInSeconds, pr.getPreviousTimeInSeconds());
                }
            } else {
                PersonalRecord pr = PersonalRecord.builder()
                        .user(activity.getUser())
                        .activity(activity)
                        .activityType(activity.getActivityType())
                        .distanceType(distanceType)
                        .timeInSeconds(timeInSeconds)
                        .distance(distanceType.getDistanceKm())
                        .achievedAt(activity.getStartTime())
                        .build();
                PersonalRecord saved = personalRecordRepository.save(pr);
                updatedPRs.add(saved);
                log.info("First PR for user {}: {} {} in {}s",
                        activity.getUser().getId(), activity.getActivityType(),
                        distanceType, timeInSeconds);
            }
        }

        return updatedPRs;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonalRecord> getUserPRs(Long userId) {
        return personalRecordRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonalRecord> getUserPRsVisibleTo(Long targetUserId, Long viewerId) {
        if (viewerId != null && viewerId.equals(targetUserId)) {
            // Users always see their own PRs, regardless of privacy flag
            return personalRecordRepository.findByUserId(targetUserId);
        }

        boolean targetIsPrivate = userRepository.findById(targetUserId)
                .map(u -> Boolean.TRUE.equals(u.getIsPrivate()))
                .orElse(false);

        if (targetIsPrivate) {
            boolean viewerIsFollower = viewerId != null
                    && userRepository.isFollowing(targetUserId, viewerId);
            if (!viewerIsFollower) {
                return Collections.emptyList();
            }
        }

        return personalRecordRepository.findByUserId(targetUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonalRecord> getUserPRsByActivityType(Long userId, ActivityType activityType) {
        return personalRecordRepository.findByUserIdAndActivityType(userId, activityType);
    }

    @Override
    @Transactional
    public void recalculateAllPRs(Long userId) {
        personalRecordRepository.deleteByUserId(userId);

        List<Activity> activities = activityRepository.findAllByUserIdOrderByStartTimeAsc(userId);
        for (Activity activity : activities) {
            checkAndUpdatePRs(activity);
        }

        log.info("Recalculated all PRs for user {}", userId);
    }

    /**
     * Find the best times for all qualifying standard distances within an activity.
     *
     * Historically this tried a GPS "sliding window" algorithm first to find the
     * fastest sub-segment of each standard distance. That path was broken: the
     * polyline format we store is lat/lon only with NO per-point timestamps, so
     * any sub-segment time derived from it was a proportional scaling of the
     * total duration — i.e. it collapsed to pace estimation anyway when the
     * polyline was dense and uniform, and gave nonsense (e.g. a 2:50 /km pace
     * from a 5:27 /km activity) when the polyline was sparse or when the
     * decoded GPS distance diverged from the activity's stored distance.
     *
     * Until we persist per-point timestamps alongside the polyline, this just
     * does uniform-pace scaling from the activity's stored distance+duration.
     * The dense-polyline case produces identical numbers to the old algorithm;
     * the sparse/broken case now produces realistic numbers instead of
     * impossible PRs.
     */
    private Map<DistanceType, Integer> findBestTimesForActivity(Activity activity) {
        Map<DistanceType, Integer> bestTimes = new EnumMap<>(DistanceType.class);
        double activityDistanceKm = activity.getDistance();

        for (DistanceType distanceType : DistanceType.values()) {
            if (!isDistanceApplicable(distanceType, activity.getActivityType())) continue;
            if (activityDistanceKm < distanceType.getDistanceKm()) continue;

            int estimatedTime = (int) Math.round(
                    (distanceType.getDistanceKm() / activityDistanceKm) * activity.getDuration());
            if (estimatedTime > 0) {
                bestTimes.put(distanceType, estimatedTime);
            }
        }

        return bestTimes;
    }

    /**
     * Half Marathon (21.0975 km) and Marathon (42.195 km) are running-only
     * milestones. Allowing them for cycling / swimming / skiing renders as
     * "Half Marathon" under a Cycling section, which is nonsensical, and
     * uses thresholds that don't line up with those sports' real benchmarks.
     */
    private boolean isDistanceApplicable(DistanceType distanceType, ActivityType activityType) {
        if (distanceType == DistanceType.HALF_MARATHON || distanceType == DistanceType.MARATHON) {
            return LONG_RACE_ELIGIBLE.contains(activityType);
        }
        return true;
    }

}
