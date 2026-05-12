package com.strava.activityservice.pr;

import com.strava.activityservice.domain.ActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/personal-records")
@RequiredArgsConstructor
public class PersonalRecordController {

    private final PersonalRecordService service;

    @GetMapping("/me")
    public List<PersonalRecordDto> mine(@AuthenticationPrincipal Long userId,
                                        @RequestParam(value = "sport", required = false) ActivityType sport) {
        return sport == null ? service.listForUser(userId) : service.listForUserAndSport(userId, sport);
    }

    @GetMapping("/user/{userId}")
    public List<PersonalRecordDto> forUser(@PathVariable Long userId,
                                           @RequestParam(value = "sport", required = false) ActivityType sport) {
        return sport == null ? service.listForUser(userId) : service.listForUserAndSport(userId, sport);
    }
}
