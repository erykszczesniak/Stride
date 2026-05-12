package com.strava.activityservice.activity;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ActivityDto create(@AuthenticationPrincipal Long userId,
                              @Valid @RequestBody CreateActivityRequest req) {
        return service.create(userId, req);
    }

    @GetMapping
    public Page<ActivityDto> listMine(@AuthenticationPrincipal Long userId, Pageable pageable) {
        return service.listForUser(userId, pageable);
    }

    @GetMapping("/user/{userId}")
    public Page<ActivityDto> listByUser(@PathVariable Long userId, Pageable pageable) {
        return service.listForUser(userId, pageable);
    }

    @GetMapping("/{id}")
    public ActivityDto get(@AuthenticationPrincipal Long requesterId, @PathVariable Long id) {
        return service.get(id, requesterId);
    }

    @PatchMapping("/{id}")
    public ActivityDto update(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                              @Valid @RequestBody UpdateActivityRequest req) {
        return service.update(id, userId, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        service.delete(id, userId);
    }
}
