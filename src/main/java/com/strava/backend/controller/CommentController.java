package com.strava.backend.controller;

import com.strava.backend.dto.activity.CommentRequest;
import com.strava.backend.dto.activity.CommentResponse;
import com.strava.backend.dto.activity.ReactionRequest;
import com.strava.backend.mapper.CommonMapper;
import com.strava.backend.security.UserPrincipal;
import com.strava.backend.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.strava.backend.constants.PathConstants.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(API_V1_ACTIVITIES)
public class CommentController {

    private final CommentService commentService;
    private final CommonMapper commonMapper;

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) authentication.getPrincipal()).getId();
        }
        return null;
    }

    @GetMapping(ACTIVITY_ID + COMMENTS)
    public ResponseEntity<List<CommentResponse>> getActivityComments(
            @PathVariable Long activityId) {
        var comments = commentService.getCommentsByActivityId(activityId);
        return ResponseEntity.ok(commonMapper.convertToResponseList(comments, CommentResponse.class));
    }

    @PostMapping(ACTIVITY_ID + COMMENTS)
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long activityId,
            @Valid @RequestBody CommentRequest request) {
        Long currentUserId = getAuthenticatedUserId();
        var comment = commentService.createComment(activityId, currentUserId, request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commonMapper.convertToResponse(comment, CommentResponse.class));
    }

    @PutMapping(ACTIVITY_ID + COMMENTS + COMMENT_ID)
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long activityId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request) {
        Long currentUserId = getAuthenticatedUserId();
        var comment = commentService.updateComment(commentId, currentUserId, request.getContent());
        return ResponseEntity.ok(commonMapper.convertToResponse(comment, CommentResponse.class));
    }

    @DeleteMapping(ACTIVITY_ID + COMMENTS + COMMENT_ID)
    public ResponseEntity<Map<String, String>> deleteComment(
            @PathVariable Long activityId,
            @PathVariable Long commentId) {
        Long currentUserId = getAuthenticatedUserId();
        commentService.deleteComment(commentId, currentUserId);
        return ResponseEntity.ok(Map.of("message", "Comment deleted"));
    }

    @PostMapping(ACTIVITY_ID + COMMENTS + COMMENT_ID + REACTIONS)
    public ResponseEntity<Map<String, Object>> addReaction(
            @PathVariable Long activityId,
            @PathVariable Long commentId,
            @Valid @RequestBody ReactionRequest request) {
        Long currentUserId = getAuthenticatedUserId();
        commentService.addReaction(commentId, currentUserId, request.getEmoji());
        var counts = commentService.getReactionCounts(commentId);
        var userReactions = commentService.getUserReactions(commentId, currentUserId);
        return ResponseEntity.ok(Map.of(
                "message", "Reaction added",
                "reactions", counts,
                "userReactions", userReactions
        ));
    }

    @DeleteMapping(ACTIVITY_ID + COMMENTS + COMMENT_ID + REACTIONS)
    public ResponseEntity<Map<String, Object>> removeReaction(
            @PathVariable Long activityId,
            @PathVariable Long commentId,
            @Valid @RequestBody ReactionRequest request) {
        Long currentUserId = getAuthenticatedUserId();
        commentService.removeReaction(commentId, currentUserId, request.getEmoji());
        var counts = commentService.getReactionCounts(commentId);
        var userReactions = commentService.getUserReactions(commentId, currentUserId);
        return ResponseEntity.ok(Map.of(
                "message", "Reaction removed",
                "reactions", counts,
                "userReactions", userReactions
        ));
    }

    @GetMapping(ACTIVITY_ID + COMMENTS + COMMENT_ID + REACTIONS)
    public ResponseEntity<Map<String, Object>> getReactions(
            @PathVariable Long activityId,
            @PathVariable Long commentId) {
        Long currentUserId = getAuthenticatedUserId();
        var counts = commentService.getReactionCounts(commentId);
        var userReactions = currentUserId != null
                ? commentService.getUserReactions(commentId, currentUserId)
                : List.of();
        return ResponseEntity.ok(Map.of(
                "reactions", counts,
                "userReactions", userReactions
        ));
    }
}
