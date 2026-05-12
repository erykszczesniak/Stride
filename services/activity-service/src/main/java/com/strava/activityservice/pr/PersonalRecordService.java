package com.strava.activityservice.pr;

import com.strava.activityservice.domain.*;
import com.strava.activityservice.event.ActivityEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Personal record computation.
 *
 * For each completed activity, walk every {@link DistanceType} that applies to
 * the sport (pool distances on swim only, half/full marathon on run only, etc.)
 * If the activity covered at least that distance, compute the elapsed time at
 * that distance using a uniform-pace assumption (totalSeconds * targetKm / totalKm).
 * Compare against any existing PR for (user, sport, distance) and persist a new
 * record if it's faster — emitting a PersonalRecordAchievedEvent on Kafka so
 * notification-service can react.
 *
 * Real per-km splits computed from per-trackpoint timestamps live in a follow-up
 * (Phase 3b/3c) — they require the GPS streams that are not yet modelled.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalRecordService {

    private final PersonalRecordRepository records;
    private final ActivityEventPublisher events;

    @Transactional(readOnly = true)
    public List<PersonalRecordDto> listForUser(Long userId) {
        return records.findAllByUserId(userId).stream().map(PersonalRecordDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PersonalRecordDto> listForUserAndSport(Long userId, ActivityType sport) {
        return records.findAllByUserIdAndActivityType(userId, sport).stream()
                .map(PersonalRecordDto::from).toList();
    }

    @Transactional
    public void computeAndPersist(Activity activity) {
        if (!activity.getActivityType().isDistanceSport()) return;
        if (activity.getDistance() == null || activity.getDistance() <= 0) return;
        if (activity.getDuration() == null || activity.getDuration() <= 0) return;

        for (DistanceType d : DistanceType.values()) {
            if (!d.isApplicableTo(activity.getActivityType())) continue;
            if (activity.getDistance() < d.distanceKm) continue;

            int timeAtDistance = (int) Math.round(
                    activity.getDuration().doubleValue() * d.distanceKm / activity.getDistance());

            Optional<PersonalRecord> existing = records.findByUserIdAndActivityTypeAndDistanceType(
                    activity.getUserId(), activity.getActivityType(), d);

            if (existing.isPresent()) {
                PersonalRecord current = existing.get();
                if (timeAtDistance >= current.getTimeInSeconds()) continue;
                int previousTime = current.getTimeInSeconds();
                current.setActivityId(activity.getId());
                current.setTimeInSeconds(timeAtDistance);
                current.setDistance(d.distanceKm);
                current.setAchievedAt(activity.getStartTime());
                current.setPreviousTimeInSeconds(previousTime);
                events.publishPersonalRecordAchieved(current);
                log.debug("Updated PR userId={} sport={} dist={} time={}s (was {}s)",
                        activity.getUserId(), activity.getActivityType(), d, timeAtDistance, previousTime);
            } else {
                PersonalRecord fresh = PersonalRecord.builder()
                        .userId(activity.getUserId())
                        .activityId(activity.getId())
                        .activityType(activity.getActivityType())
                        .distanceType(d)
                        .timeInSeconds(timeAtDistance)
                        .distance(d.distanceKm)
                        .achievedAt(activity.getStartTime())
                        .build();
                records.save(fresh);
                events.publishPersonalRecordAchieved(fresh);
                log.debug("New PR userId={} sport={} dist={} time={}s",
                        activity.getUserId(), activity.getActivityType(), d, timeAtDistance);
            }
        }
    }
}
