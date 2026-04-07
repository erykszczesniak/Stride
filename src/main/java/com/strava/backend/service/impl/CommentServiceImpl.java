package com.strava.backend.service.impl;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.Comment;
import com.strava.backend.domain.CommentReaction;
import com.strava.backend.domain.User;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.enums.NotificationType;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.repository.CommentReactionRepository;
import com.strava.backend.repository.CommentRepository;
import com.strava.backend.repository.UserRepository;
import com.strava.backend.service.CommentService;
import com.strava.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.strava.backend.constants.ErrorMessage.*;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final CommentReactionRepository commentReactionRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public List<Comment> getCommentsByActivityId(Long activityId) {
        if (!activityRepository.existsById(activityId)) {
            throw new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        return commentRepository.findByActivityIdOrderByCreatedAtDesc(activityId);
    }

    @Override
    @Transactional(readOnly = true)
    public Comment getCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new ApiRequestException(COMMENT_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    @Override
    @Transactional
    public Comment createComment(Long activityId, Long userId, String content) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));

        Comment comment = Comment.builder()
                .content(content)
                .activity(activity)
                .user(user)
                .build();

        Comment savedComment = commentRepository.save(comment);

        notificationService.createNotification(
                activity.getUser(),
                user,
                NotificationType.COMMENT,
                user.getFirstName() + " " + user.getLastName() + " commented on your activity \"" + activity.getTitle() + "\"",
                activityId,
                "ACTIVITY"
        );

        return savedComment;
    }

    @Override
    @Transactional
    public Comment updateComment(Long commentId, Long userId, String content) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ApiRequestException(COMMENT_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (!comment.getUser().getId().equals(userId)) {
            throw new ApiRequestException(ACCESS_DENIED, HttpStatus.FORBIDDEN);
        }

        comment.setContent(content);
        return commentRepository.save(comment);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ApiRequestException(COMMENT_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (!comment.getUser().getId().equals(userId)) {
            throw new ApiRequestException(ACCESS_DENIED, HttpStatus.FORBIDDEN);
        }

        commentRepository.delete(comment);
    }

    @Override
    @Transactional
    public CommentReaction addReaction(Long commentId, Long userId, String emoji) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ApiRequestException(COMMENT_NOT_FOUND, HttpStatus.NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));

        var existingReaction = commentReactionRepository.findByCommentIdAndUserIdAndEmoji(commentId, userId, emoji);
        if (existingReaction.isPresent()) {
            return existingReaction.get();
        }

        CommentReaction reaction = CommentReaction.builder()
                .emoji(emoji)
                .comment(comment)
                .user(user)
                .build();

        return commentReactionRepository.save(reaction);
    }

    @Override
    @Transactional
    public void removeReaction(Long commentId, Long userId, String emoji) {
        var reaction = commentReactionRepository.findByCommentIdAndUserIdAndEmoji(commentId, userId, emoji)
                .orElseThrow(() -> new ApiRequestException("Reaction not found", HttpStatus.NOT_FOUND));

        commentReactionRepository.delete(reaction);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getReactionCounts(Long commentId) {
        List<Object[]> results = commentReactionRepository.countReactionsByCommentId(commentId);
        Map<String, Long> counts = new HashMap<>();
        for (Object[] result : results) {
            counts.put((String) result[0], (Long) result[1]);
        }
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getUserReactions(Long commentId, Long userId) {
        return commentReactionRepository.findUserReactionsByCommentId(commentId, userId);
    }
}
