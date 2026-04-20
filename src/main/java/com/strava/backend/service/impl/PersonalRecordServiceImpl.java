package com.strava.backend.service.impl;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.PersonalRecord;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.enums.DistanceType;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.repository.PersonalRecordRepository;
import com.strava.backend.repository.UserRepository;
import com.strava.backend.service.PersonalRecordService;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
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
    private static final Map<ActivityType, Set<DistanceType>> DISTANCES_BY_SPORT;
    static {
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

        Map<ActivityType, Set<DistanceType>> map = new EnumMap<>(ActivityType.class);
        map.put(ActivityType.RUN, runningSet);
        map.put(ActivityType.VIRTUAL_RUN, runningSet);
        map.put(ActivityType.WALK, runningSet);
        map.put(ActivityType.HIKE, runningSet);
        map.put(ActivityType.RIDE, cyclingSet);
        map.put(ActivityType.VIRTUAL_RIDE, cyclingSet);
        map.put(ActivityType.SWIM, swimmingSet);
        map.put(ActivityType.ROWING, genericSet);
        map.put(ActivityType.KAYAKING, genericSet);
        map.put(ActivityType.STAND_UP_PADDLING, genericSet);
        map.put(ActivityType.SURFING, genericSet);
        map.put(ActivityType.NORDIC_SKI, genericSet);
        map.put(ActivityType.ALPINE_SKI, genericSet);
        map.put(ActivityType.BACKCOUNTRY_SKI, genericSet);
        map.put(ActivityType.ROLLER_SKI, genericSet);
        map.put(ActivityType.ICE_SKATE, genericSet);
        map.put(ActivityType.INLINE_SKATE, genericSet);
        map.put(ActivityType.SNOWBOARD, genericSet);
        map.put(ActivityType.SNOWSHOE, genericSet);
        DISTANCES_BY_SPORT = Collections.unmodifiableMap(map);
    }

    private final PersonalRecordRepository personalRecordRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final ObservationRegistry observationRegistry;

    @Override
    @Transactional
    public List<PersonalRecord> checkAndUpdatePRs(Activity activity) {
        if (activity.getDistance() == null || activity.getDuration() == null
                || activity.getDistance() <= 0 || activity.getDuration() <= 0) {
            return Collections.emptyList();
        }

        if (!DISTANCES_BY_SPORT.containsKey(activity.getActivityType())) {
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

    /**
     * Recalculate every PR from scratch. This method is manually instrumented
     * with Micrometer {@link Observation}s so that the trace in Jaeger shows
     * two child spans under the parent HTTP span — one for the DELETE, one
     * for the per-activity recompute loop. Each {@code checkAndUpdatePRs}
     * call inside the loop also emits JDBC spans via the auto-instrumentation,
     * so anyone looking at the trace can see exactly where the time goes.
     *
     * We use {@link ObservationRegistry} (Micrometer) rather than OTel's
     * {@code @WithSpan} annotation because the annotation requires the OTel
     * Java agent to be on the JVM classpath, which Stride does not run.
     */
    @Override
    @Transactional
    public void recalculateAllPRs(Long userId) {
        Observation outer = Observation.createNotStarted("personal-record.recalculateAllPRs", observationRegistry)
                .lowCardinalityKeyValue("user.id", String.valueOf(userId))
                .start();
        try (var ignoredScope = outer.openScope()) {
            Observation.createNotStarted("personal-record.delete-existing", observationRegistry)
                    .lowCardinalityKeyValue("user.id", String.valueOf(userId))
                    .observe(() -> personalRecordRepository.deleteByUserId(userId));

            List<Activity> activities = activityRepository.findAllByUserIdOrderByStartTimeAsc(userId);

            Observation.createNotStarted("personal-record.recompute-loop", observationRegistry)
                    .lowCardinalityKeyValue("user.id", String.valueOf(userId))
                    .lowCardinalityKeyValue("activity.count", String.valueOf(activities.size()))
                    .observe(() -> {
                        for (Activity activity : activities) {
                            checkAndUpdatePRs(activity);
                        }
                    });
        } finally {
            outer.stop();
        }

        log.info("Recalculated all PRs for user {}", userId);
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
    private Map<DistanceType, Integer> findBestTimesForActivity(Activity activity) {
        Map<DistanceType, Integer> bestTimes = new EnumMap<>(DistanceType.class);
        double activityDistanceKm = activity.getDistance();

        DistanceType largestFit = null;
        for (DistanceType distanceType : DistanceType.values()) {
            if (!isDistanceApplicable(distanceType, activity.getActivityType())) continue;
            if (activityDistanceKm < distanceType.getDistanceKm()) continue;
            if (largestFit == null
                    || distanceType.getDistanceKm() > largestFit.getDistanceKm()) {
                largestFit = distanceType;
            }
        }

        if (largestFit != null) {
            int estimatedTime = (int) Math.round(
                    (largestFit.getDistanceKm() / activityDistanceKm) * activity.getDuration());
            if (estimatedTime > 0) {
                bestTimes.put(largestFit, estimatedTime);
            }
        }

        return bestTimes;
    }

/**
     * Whether a given distance type should be tracked for the given sport.
     * Reads the per-sport DISTANCES_BY_SPORT map — e.g. Swimming gets
     * 50m/100m/200m/400m/1km/3km, Cycling gets 1/5/10 km only, etc.
     */
    private boolean isDistanceApplicable(DistanceType distanceType, ActivityType activityType) {
        Set<DistanceType> allowed = DISTANCES_BY_SPORT.get(activityType);
        return allowed != null && allowed.contains(distanceType);
    }

}
