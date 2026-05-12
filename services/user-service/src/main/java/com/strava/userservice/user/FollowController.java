package com.strava.userservice.user;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{id}")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService service;

    @PostMapping("/follow")
    public ResponseEntity<Void> follow(@AuthenticationPrincipal Long followerId, @PathVariable Long id) {
        service.follow(followerId, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/follow")
    public ResponseEntity<Void> unfollow(@AuthenticationPrincipal Long followerId, @PathVariable Long id) {
        service.unfollow(followerId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/followers")
    public Page<FollowService.UserSummaryDto> followers(@PathVariable Long id, Pageable pageable) {
        return service.followers(id, pageable);
    }

    @GetMapping("/following")
    public Page<FollowService.UserSummaryDto> following(@PathVariable Long id, Pageable pageable) {
        return service.following(id, pageable);
    }
}
