package com.strava.socialservice.feed;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Feed read endpoints, ported from main's ActivityController: plain JSON
 * array bodies of ActivityResponse plus page-total-count / page-total-elements
 * headers (HeaderResponse envelope), default page size 10.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/activities")
public class FeedController {

    private final FeedQueryService feedQueryService;

    @GetMapping("/feed")
    public ResponseEntity<List<ActivityResponse>> getFeedActivities(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10) Pageable pageable) {
        var response = feedQueryService.getFeed(userId, pageable);
        return ResponseEntity.ok()
                .headers(response.getHeaders())
                .body(response.getItems());
    }

    @GetMapping("/public")
    public ResponseEntity<List<ActivityResponse>> getPublicActivities(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10) Pageable pageable) {
        var response = feedQueryService.getPublicActivities(userId, pageable);
        return ResponseEntity.ok()
                .headers(response.getHeaders())
                .body(response.getItems());
    }
}
