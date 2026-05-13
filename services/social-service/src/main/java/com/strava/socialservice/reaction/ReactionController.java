package com.strava.socialservice.reaction;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reactions")
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService service;

    @PostMapping("/activity/{activityId}")
    public ReactionDto react(@AuthenticationPrincipal Long userId,
                             @PathVariable Long activityId,
                             @RequestParam(value = "type", required = false) String type) {
        return service.react(activityId, userId, type);
    }

    @DeleteMapping("/activity/{activityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unreact(@AuthenticationPrincipal Long userId,
                        @PathVariable Long activityId,
                        @RequestParam(value = "type", required = false) String type) {
        service.unreact(activityId, userId, type);
    }

    @GetMapping("/activity/{activityId}")
    public Map<String, Object> summary(@PathVariable Long activityId) {
        long kudos = service.count(activityId, ReactionService.KUDOS);
        List<ReactionDto> all = service.listForActivity(activityId);
        return Map.of("kudosCount", kudos, "reactions", all);
    }
}
