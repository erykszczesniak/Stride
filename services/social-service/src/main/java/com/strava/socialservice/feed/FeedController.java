package com.strava.socialservice.feed;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService service;

    @GetMapping
    public Page<FeedItemDto> global(Pageable pageable) {
        return service.globalFeed(pageable);
    }

    @GetMapping("/user/{userId}")
    public Page<FeedItemDto> byUser(@PathVariable Long userId, Pageable pageable) {
        return service.userFeed(userId, pageable);
    }
}
