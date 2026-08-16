package com.strava.socialservice.like;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Like endpoints, ported 1:1 from main's ActivityController (paths + bodies). */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/activities")
public class LikeController {

    private final LikeService likeService;

    @PostMapping("/{activityId}/like")
    public ResponseEntity<Map<String, String>> likeActivity(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long activityId) {
        likeService.likeActivity(activityId, userId);
        return ResponseEntity.ok(Map.of("message", "Activity liked"));
    }

    @DeleteMapping("/{activityId}/like")
    public ResponseEntity<Map<String, String>> unlikeActivity(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long activityId) {
        likeService.unlikeActivity(activityId, userId);
        return ResponseEntity.ok(Map.of("message", "Activity unliked"));
    }
}
