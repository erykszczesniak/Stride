package com.strava.backend.controller;

import com.strava.backend.dto.activity.CommentRequest;
import com.strava.backend.dto.activity.CommentResponse;
import com.strava.backend.mapper.CommonMapper;
import com.strava.backend.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.strava.backend.constants.PathConstants.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(API_V1_ACTIVITIES)
public class CommentController {

    private static final Long DEFAULT_USER_ID = 1L;

    private final CommentService commentService;
    private final CommonMapper commonMapper;

    @GetMapping(ACTIVITY_ID + COMMENTS)
    public ResponseEntity<List<CommentResponse>> getActivityComments(
            @PathVariable Long activityId) {
        var comments = commentService.getCommentsByActivityId(activityId);
        return ResponseEntity.ok(commonMapper.convertToResponseList(comments, CommentResponse.class));
    }

    @PostMapping(ACTIVITY_ID + COMMENTS)
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long activityId,
            @RequestParam(required = false) Long userId,
            @Valid @RequestBody CommentRequest request) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
        var comment = commentService.createComment(activityId, currentUserId, request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commonMapper.convertToResponse(comment, CommentResponse.class));
    }

    @PutMapping(ACTIVITY_ID + COMMENTS + COMMENT_ID)
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long activityId,
            @PathVariable Long commentId,
            @RequestParam(required = false) Long userId,
            @Valid @RequestBody CommentRequest request) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
        var comment = commentService.updateComment(commentId, currentUserId, request.getContent());
        return ResponseEntity.ok(commonMapper.convertToResponse(comment, CommentResponse.class));
    }

    @DeleteMapping(ACTIVITY_ID + COMMENTS + COMMENT_ID)
    public ResponseEntity<Map<String, String>> deleteComment(
            @PathVariable Long activityId,
            @PathVariable Long commentId,
            @RequestParam(required = false) Long userId) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
        commentService.deleteComment(commentId, currentUserId);
        return ResponseEntity.ok(Map.of("message", "Comment deleted"));
    }
}
