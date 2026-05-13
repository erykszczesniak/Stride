package com.strava.socialservice.comment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService service;

    @PostMapping("/activity/{activityId}")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto create(@AuthenticationPrincipal Long userId,
                             @PathVariable Long activityId,
                             @Valid @RequestBody CreateCommentRequest req) {
        return service.create(activityId, userId, req);
    }

    @GetMapping("/activity/{activityId}")
    public Page<CommentDto> list(@PathVariable Long activityId, Pageable pageable) {
        return service.list(activityId, pageable);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        service.delete(id, userId);
    }
}
