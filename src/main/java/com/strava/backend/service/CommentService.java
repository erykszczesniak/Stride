package com.strava.backend.service;

import com.strava.backend.domain.Comment;
import com.strava.backend.domain.CommentReaction;

import java.util.List;
import java.util.Map;

public interface CommentService {

    List<Comment> getCommentsByActivityId(Long activityId);

    Comment getCommentById(Long commentId);

    Comment createComment(Long activityId, Long userId, String content);

    Comment updateComment(Long commentId, Long userId, String content);

    void deleteComment(Long commentId, Long userId);

    CommentReaction addReaction(Long commentId, Long userId, String emoji);

    void removeReaction(Long commentId, Long userId, String emoji);

    Map<String, Long> getReactionCounts(Long commentId);

    List<String> getUserReactions(Long commentId, Long userId);
}
