package com.strava.backend.mapper;

import com.strava.backend.domain.PersonalRecord;
import com.strava.backend.dto.personalrecord.PersonalRecordResponse;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.service.PersonalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PersonalRecordMapper {

    private final PersonalRecordService personalRecordService;

    public List<PersonalRecordResponse> getUserPRs(Long userId) {
        return personalRecordService.getUserPRs(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<PersonalRecordResponse> getUserPRsByActivityType(Long userId, ActivityType activityType) {
        return personalRecordService.getUserPRsByActivityType(userId, activityType).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public void recalculateAllPRs(Long userId) {
        personalRecordService.recalculateAllPRs(userId);
    }

    private PersonalRecordResponse mapToResponse(PersonalRecord pr) {
        PersonalRecordResponse.PersonalRecordResponseBuilder builder = PersonalRecordResponse.builder()
                .id(pr.getId())
                .activityType(pr.getActivityType())
                .distanceType(pr.getDistanceType())
                .distanceDisplayName(pr.getDistanceType().getDisplayName())
                .distance(pr.getDistance())
                .timeInSeconds(pr.getTimeInSeconds())
                .formattedTime(formatTime(pr.getTimeInSeconds()))
                .achievedAt(pr.getAchievedAt())
                .activityId(pr.getActivity().getId())
                .activityTitle(pr.getActivity().getTitle())
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
