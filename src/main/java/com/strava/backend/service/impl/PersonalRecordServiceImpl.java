package com.strava.backend.service.impl;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.PersonalRecord;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.enums.DistanceType;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.repository.PersonalRecordRepository;
import com.strava.backend.service.PersonalRecordService;
import com.strava.backend.service.importer.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalRecordServiceImpl implements PersonalRecordService {

    private final PersonalRecordRepository personalRecordRepository;
    private final ActivityRepository activityRepository;

    @Override
    @Transactional
    public List<PersonalRecord> checkAndUpdatePRs(Activity activity) {
        if (activity.getDistance() == null || activity.getDuration() == null
                || activity.getDistance() <= 0 || activity.getDuration() <= 0) {
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
     * Uses GPS segment analysis when polyline data is available, otherwise estimates from pace.
     */
    private Map<DistanceType, Integer> findBestTimesForActivity(Activity activity) {
        Map<DistanceType, Integer> bestTimes = new EnumMap<>(DistanceType.class);
        double activityDistanceKm = activity.getDistance();

        // Try GPS-based segment analysis first
        if (activity.getRoutePolyline() != null && !activity.getRoutePolyline().isEmpty()) {
            Map<DistanceType, Integer> gpsTimes = findBestTimesFromPolyline(activity);
            if (!gpsTimes.isEmpty()) {
                bestTimes.putAll(gpsTimes);
            }
        }

        // Fallback: estimate from overall pace for distances not found via GPS
        for (DistanceType distanceType : DistanceType.values()) {
            if (bestTimes.containsKey(distanceType)) continue;

            if (activityDistanceKm >= distanceType.getDistanceKm()) {
                int estimatedTime = (int) Math.round(
                        (distanceType.getDistanceKm() / activityDistanceKm) * activity.getDuration());
                if (estimatedTime > 0) {
                    bestTimes.put(distanceType, estimatedTime);
                }
            }
        }

        return bestTimes;
    }

    /**
     * Sliding window algorithm on decoded polyline to find fastest segments.
     * For each standard distance, finds the minimum time window covering that distance.
     * Complexity: O(n * d) where n = GPS points, d = number of distance types.
     */
    private Map<DistanceType, Integer> findBestTimesFromPolyline(Activity activity) {
        Map<DistanceType, Integer> bestTimes = new EnumMap<>(DistanceType.class);

        List<double[]> decodedPoints = GeoUtils.decodePolyline(activity.getRoutePolyline());
        if (decodedPoints.size() < 2) return bestTimes;

        // Calculate cumulative distances between points (in km)
        int n = decodedPoints.size();
        double[] cumulativeDistKm = new double[n];
        cumulativeDistKm[0] = 0;
        for (int i = 1; i < n; i++) {
            double[] prev = decodedPoints.get(i - 1);
            double[] curr = decodedPoints.get(i);
            double segmentKm = GeoUtils.haversineMeters(prev[0], prev[1], curr[0], curr[1]) / 1000.0;
            cumulativeDistKm[i] = cumulativeDistKm[i - 1] + segmentKm;
        }

        double totalGpsDistKm = cumulativeDistKm[n - 1];
        if (totalGpsDistKm <= 0) return bestTimes;

        // Estimate time per point by distributing total duration evenly across distance
        int totalDuration = activity.getDuration();

        for (DistanceType distanceType : DistanceType.values()) {
            double targetKm = distanceType.getDistanceKm();
            if (totalGpsDistKm < targetKm) continue;

            // Sliding window: two pointers
            int bestTimeForDistance = Integer.MAX_VALUE;
            int right = 0;

            for (int left = 0; left < n; left++) {
                // Advance right pointer until segment distance >= target
                while (right < n - 1 && (cumulativeDistKm[right] - cumulativeDistKm[left]) < targetKm) {
                    right++;
                }

                double segmentDistKm = cumulativeDistKm[right] - cumulativeDistKm[left];
                if (segmentDistKm >= targetKm) {
                    // Estimate time for this segment based on distance proportion
                    double startFraction = cumulativeDistKm[left] / totalGpsDistKm;
                    double endFraction = cumulativeDistKm[right] / totalGpsDistKm;
                    int segmentTime = (int) Math.round((endFraction - startFraction) * totalDuration);

                    // Adjust for the overshoot (segment may be slightly longer than target)
                    double ratio = targetKm / segmentDistKm;
                    int adjustedTime = (int) Math.round(segmentTime * ratio);

                    if (adjustedTime > 0 && adjustedTime < bestTimeForDistance) {
                        bestTimeForDistance = adjustedTime;
                    }
                }
            }

            if (bestTimeForDistance < Integer.MAX_VALUE) {
                bestTimes.put(distanceType, bestTimeForDistance);
            }
        }

        return bestTimes;
    }
}
