package com.strava.socialservice.comment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Nested comment + emoji-reaction contract, ported 1:1 from main's
 * CommentController (paths, verbs, status codes and response shapes).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/activities")
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/{activityId}/comments")
    public ResponseEntity<List<CommentResponse>> getActivityComments(@PathVariable Long activityId) {
        return ResponseEntity.ok(commentService.getCommentsByActivityId(activityId));
    }

    @PostMapping("/{activityId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long activityId,
            @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.createComment(activityId, userId, request.getContent()));
    }

    @PutMapping("/{activityId}/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long activityId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.ok(commentService.updateComment(commentId, userId, request.getContent()));
    }

    @DeleteMapping("/{activityId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long activityId,
            @PathVariable Long commentId) {
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{activityId}/comments/{commentId}/reactions")
    public ResponseEntity<Map<String, Object>> addReaction(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long activityId,
            @PathVariable Long commentId,
            @Valid @RequestBody ReactionRequest request) {
        commentService.addReaction(commentId, userId, request.getEmoji());
        return ResponseEntity.ok(Map.of(
                "message", "Reaction added",
                "reactions", commentService.getReactionCounts(commentId),
                "userReactions", commentService.getUserReactions(commentId, userId)
        ));
    }

    @DeleteMapping("/{activityId}/comments/{commentId}/reactions")
    public ResponseEntity<Map<String, Object>> removeReaction(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long activityId,
            @PathVariable Long commentId,
            @Valid @RequestBody ReactionRequest request) {
        commentService.removeReaction(commentId, userId, request.getEmoji());
        return ResponseEntity.ok(Map.of(
                "message", "Reaction removed",
                "reactions", commentService.getReactionCounts(commentId),
                "userReactions", commentService.getUserReactions(commentId, userId)
        ));
    }

    @GetMapping("/{activityId}/comments/{commentId}/reactions")
    public ResponseEntity<Map<String, Object>> getReactions(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long activityId,
            @PathVariable Long commentId) {
        return ResponseEntity.ok(Map.of(
                "reactions", commentService.getReactionCounts(commentId),
                "userReactions", userId != null
                        ? commentService.getUserReactions(commentId, userId)
                        : List.of()
        ));
    }
}
