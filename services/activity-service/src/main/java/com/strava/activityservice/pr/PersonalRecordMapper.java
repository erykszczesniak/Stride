package com.strava.activityservice.pr;

import com.strava.activityservice.domain.Activity;
import com.strava.activityservice.domain.ActivityRepository;
import com.strava.activityservice.domain.ActivityType;
import com.strava.activityservice.domain.PersonalRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Ported from main's PersonalRecordMapper. Activity titles are resolved with
 * one batched findAllById per response (the PR entity keeps a plain
 * activityId, no JPA association).
 */
@Component
@RequiredArgsConstructor
public class PersonalRecordMapper {

    private final PersonalRecordService personalRecordService;
    private final ActivityRepository activityRepository;

    public List<PersonalRecordResponse> getUserPRs(Long userId) {
        return mapAll(personalRecordService.getUserPRs(userId));
    }

    public List<PersonalRecordResponse> getUserPRsVisibleTo(Long targetUserId, Long viewerId) {
        return mapAll(personalRecordService.getUserPRsVisibleTo(targetUserId, viewerId));
    }

    public List<PersonalRecordResponse> getUserPRsByActivityType(Long userId, ActivityType activityType) {
        return mapAll(personalRecordService.getUserPRsByActivityType(userId, activityType));
    }

    public void recalculateAllPRs(Long userId) {
        personalRecordService.recalculateAllPRs(userId);
    }

    private List<PersonalRecordResponse> mapAll(List<PersonalRecord> records) {
        List<Long> activityIds = records.stream().map(PersonalRecord::getActivityId).distinct().toList();
        Map<Long, String> titles = activityRepository.findAllById(activityIds).stream()
                .collect(Collectors.toMap(Activity::getId, Activity::getTitle));
        return records.stream()
                .map(pr -> mapToResponse(pr, titles.get(pr.getActivityId())))
                .collect(Collectors.toList());
    }

    private PersonalRecordResponse mapToResponse(PersonalRecord pr, String activityTitle) {
        PersonalRecordResponse.PersonalRecordResponseBuilder builder = PersonalRecordResponse.builder()
                .id(pr.getId())
                .activityType(pr.getActivityType())
                .distanceType(pr.getDistanceType())
                .distance(pr.getDistance())
                .timeInSeconds(pr.getTimeInSeconds())
                .formattedTime(formatTime(pr.getTimeInSeconds()))
                .achievedAt(pr.getAchievedAt())
                .activityId(pr.getActivityId())
                .activityTitle(activityTitle)
                .previousTimeInSeconds(pr.getPreviousTimeInSeconds());

        // Calculate pace (min/km)
        if (pr.getDistance() > 0 && pr.getTimeInSeconds() > 0) {
            double paceSeconds = pr.getTimeInSeconds() / pr.getDistance();
            builder.formattedPace(formatPace(paceSeconds));
            builder.speedKmh(Math.round((pr.getDistance() / pr.getTimeInSeconds() * 3600) * 10.0) / 10.0);
        }

        // Calculate improvement
        if (pr.getPreviousTimeInSeconds() != null) {
            int improvement = pr.getPreviousTimeInSeconds() - pr.getTimeInSeconds();
            builder.improvementSeconds(improvement);
            double percent = ((double) improvement / pr.getPreviousTimeInSeconds()) * 100;
            builder.improvementPercent(Math.round(percent * 10.0) / 10.0);
        }

        return builder.build();
    }

    private String formatTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%d:%02d", minutes, seconds);
    }

    private String formatPace(double secondsPerKm) {
        int totalSeconds = (int) Math.round(secondsPerKm);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d /km", minutes, seconds);
    }
}
