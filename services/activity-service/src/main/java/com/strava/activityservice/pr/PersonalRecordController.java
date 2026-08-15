package com.strava.activityservice.pr;

import com.strava.activityservice.domain.ActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Public /api/v1/personal-records surface, mirroring main's controller. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/personal-records")
public class PersonalRecordController {

    private final PersonalRecordMapper personalRecordMapper;

    @GetMapping
    public ResponseEntity<List<PersonalRecordResponse>> getMyPRs(@AuthenticationPrincipal Long currentUserId) {
        return ResponseEntity.ok(personalRecordMapper.getUserPRs(currentUserId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PersonalRecordResponse>> getUserPRs(
            @AuthenticationPrincipal Long viewerId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(personalRecordMapper.getUserPRsVisibleTo(userId, viewerId));
    }

    @GetMapping("/activity-type/{activityType}")
    public ResponseEntity<List<PersonalRecordResponse>> getMyPRsByActivityType(
            @AuthenticationPrincipal Long currentUserId,
            @PathVariable ActivityType activityType) {
        return ResponseEntity.ok(personalRecordMapper.getUserPRsByActivityType(currentUserId, activityType));
    }

    @PostMapping("/recalculate")
    public ResponseEntity<Map<String, String>> recalculateMyPRs(@AuthenticationPrincipal Long currentUserId) {
        personalRecordMapper.recalculateAllPRs(currentUserId);
        return ResponseEntity.ok(Map.of("message", "Personal records recalculated successfully"));
    }
}
