package com.strava.backend.controller;

import com.strava.backend.dto.personalrecord.PersonalRecordResponse;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.mapper.PersonalRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.strava.backend.constants.PathConstants.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(API_V1_PERSONAL_RECORDS)
public class PersonalRecordController extends BaseController {

    private final PersonalRecordMapper personalRecordMapper;

    @GetMapping
    public ResponseEntity<List<PersonalRecordResponse>> getMyPRs() {
        Long currentUserId = getAuthenticatedUserId();
        return ResponseEntity.ok(personalRecordMapper.getUserPRs(currentUserId));
    }

    @GetMapping(USER + USER_ID)
    public ResponseEntity<List<PersonalRecordResponse>> getUserPRs(@PathVariable Long userId) {
        return ResponseEntity.ok(personalRecordMapper.getUserPRs(userId));
    }

    @GetMapping(ACTIVITY_TYPE + "/{activityType}")
    public ResponseEntity<List<PersonalRecordResponse>> getMyPRsByActivityType(
            @PathVariable ActivityType activityType) {
        Long currentUserId = getAuthenticatedUserId();
        return ResponseEntity.ok(personalRecordMapper.getUserPRsByActivityType(currentUserId, activityType));
    }

    @PostMapping(RECALCULATE)
    public ResponseEntity<Map<String, String>> recalculateMyPRs() {
        Long currentUserId = getAuthenticatedUserId();
        personalRecordMapper.recalculateAllPRs(currentUserId);
        return ResponseEntity.ok(Map.of("message", "Personal records recalculated successfully"));
    }
}
