package com.strava.backend.service.impl;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.Comment;
import com.strava.backend.domain.User;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.repository.CommentRepository;
import com.strava.backend.repository.UserRepository;
import com.strava.backend.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.strava.backend.constants.ErrorMessage.*;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    @Override
    public List<Comment> getCommentsByActivityId(Long activityId) {
        if (!activityRepository.existsById(activityId)) {
            throw new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        return commentRepository.findByActivityIdOrderByCreatedAtDesc(activityId);
    }

    @Override
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

        return commentRepository.save(comment);
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
}
