package com.strava.activityservice.activity;

import com.strava.activityservice.domain.Activity;
import com.strava.activityservice.domain.ActivityRepository;
import com.strava.activityservice.event.ActivityEventPublisher;
import com.strava.activityservice.exception.NotFoundException;
import com.strava.activityservice.pr.PersonalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activities;
    private final PersonalRecordService personalRecords;
    private final ActivityEventPublisher events;

    @Transactional
    public ActivityDto create(Long userId, CreateActivityRequest req) {
        Activity activity = Activity.builder()
                .userId(userId)
                .title(req.title())
                .description(req.description())
                .activityType(req.activityType())
                .distance(req.distance())
                .duration(req.duration())
                .elevationGain(req.elevationGain())
                .calories(req.calories())
                .averageSpeed(req.averageSpeed())
                .maxSpeed(req.maxSpeed())
                .averageHeartRate(req.averageHeartRate())
                .maxHeartRate(req.maxHeartRate())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .startLocation(req.startLocation())
                .endLocation(req.endLocation())
                .routePolyline(req.routePolyline())
                .mapImageUrl(req.mapImageUrl())
                .isPrivate(Boolean.TRUE.equals(req.isPrivate()))
                .build();
        activities.save(activity);

        // PR computation happens synchronously inside the same TX so the activity
        // and any new records are committed together. The PR service emits its
        // own Kafka events for each new record.
        personalRecords.computeAndPersist(activity);

        events.publishActivityCreated(activity);
        return ActivityDto.from(activity);
    }

    @Transactional(readOnly = true)
    public ActivityDto get(Long activityId, Long requesterId) {
        Activity a = activities.findById(activityId)
                .orElseThrow(() -> new NotFoundException("Activity " + activityId + " not found"));
        if (a.isPrivate() && !a.getUserId().equals(requesterId)) {
            throw new NotFoundException("Activity " + activityId + " not found");
        }
        return ActivityDto.from(a);
    }

    @Transactional(readOnly = true)
    public Page<ActivityDto> listForUser(Long userId, Pageable pageable) {
        return activities.findAllByUserIdOrderByStartTimeDesc(userId, pageable).map(ActivityDto::from);
    }

    @Transactional
    public ActivityDto update(Long activityId, Long userId, UpdateActivityRequest req) {
        Activity a = activities.findByIdAndUserId(activityId, userId)
                .orElseThrow(() -> new NotFoundException("Activity " + activityId + " not found"));
        if (req.title() != null) a.setTitle(req.title());
        if (req.description() != null) a.setDescription(req.description());
        if (req.activityType() != null) a.setActivityType(req.activityType());
        if (req.distance() != null) a.setDistance(req.distance());
        if (req.duration() != null) a.setDuration(req.duration());
        if (req.elevationGain() != null) a.setElevationGain(req.elevationGain());
        if (req.calories() != null) a.setCalories(req.calories());
        if (req.isPrivate() != null) a.setPrivate(req.isPrivate());
        events.publishActivityUpdated(a);
        return ActivityDto.from(a);
    }

    @Transactional
    public void delete(Long activityId, Long userId) {
        Activity a = activities.findByIdAndUserId(activityId, userId)
                .orElseThrow(() -> new NotFoundException("Activity " + activityId + " not found"));
        activities.delete(a);
    }
}
