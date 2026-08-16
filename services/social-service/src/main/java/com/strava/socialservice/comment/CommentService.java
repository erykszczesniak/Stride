package com.strava.socialservice.comment;

import com.strava.socialservice.domain.Comment;
import com.strava.socialservice.domain.CommentReaction;
import com.strava.socialservice.domain.CommentReactionRepository;
import com.strava.socialservice.domain.CommentRepository;
import com.strava.socialservice.domain.FeedItem;
import com.strava.socialservice.domain.FeedItemRepository;
import com.strava.socialservice.event.SocialEventPublisher;
import com.strava.socialservice.exception.ApiRequestException;
import com.strava.socialservice.user.BaseUserResponse;
import com.strava.socialservice.user.UserCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.strava.socialservice.constants.ErrorMessage.*;

/**
 * Comment + emoji-reaction write side, ported from main's CommentServiceImpl.
 * Activity existence/ownership is answered by the local feed_items snapshot
 * (eventually consistent — an activity commented milliseconds after creation
 * could briefly 404 until the created event is consumed; same window the
 * monolith's AFTER_COMMIT projector had). The author is NOT re-validated
 * against user_cache: the gateway-authenticated user id always exists
 * upstream, so a cache miss only means projection lag and degrades to a
 * placeholder author instead of main's USER_NOT_FOUND.
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentReactionRepository commentReactionRepository;
    private final FeedItemRepository feedItemRepository;
    private final UserCacheService userCacheService;
    private final SocialEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByActivityId(Long activityId) {
        if (feedItemRepository.findByActivityId(activityId).isEmpty()) {
            throw new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        List<Comment> comments = commentRepository.findByActivityIdOrderByCreatedAtDesc(activityId);
        Map<Long, BaseUserResponse> authors = userCacheService.usersFor(
                comments.stream().map(Comment::getUserId).toList());
        return comments.stream()
                .map(c -> toResponse(c, authors.get(c.getUserId())))
                .toList();
    }

    @Transactional
    public CommentResponse createComment(Long activityId, Long userId, String content) {
        FeedItem activity = feedItemRepository.findByActivityId(activityId)
                .orElseThrow(() -> new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND));

        Comment comment = Comment.builder()
                .activityId(activityId)
                .userId(userId)
                .content(content)
                .build();
        commentRepository.save(comment);

        // Enriched event: notification-service renders "<author> commented on
        // your activity" without any synchronous lookups of its own.
        eventPublisher.publishCommentAdded(comment, activity.getUserId(),
                userCacheService.displayNameFor(userId));

        return toResponse(comment, userCacheService.userFor(userId));
    }

    @Transactional
    public CommentResponse updateComment(Long commentId, Long userId, String content) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ApiRequestException(COMMENT_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (!comment.getUserId().equals(userId)) {
            throw new ApiRequestException(ACCESS_DENIED, HttpStatus.FORBIDDEN);
        }

        comment.setContent(content);
        commentRepository.save(comment);
        return toResponse(comment, userCacheService.userFor(comment.getUserId()));
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ApiRequestException(COMMENT_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (!comment.getUserId().equals(userId)) {
            throw new ApiRequestException(ACCESS_DENIED, HttpStatus.FORBIDDEN);
        }

        // comment_reactions rows go with it via the FK's ON DELETE CASCADE.
        commentRepository.delete(comment);
    }

    @Transactional
    public CommentReaction addReaction(Long commentId, Long userId, String emoji) {
        if (!commentRepository.existsById(commentId)) {
            throw new ApiRequestException(COMMENT_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        return commentReactionRepository.findByCommentIdAndUserIdAndEmoji(commentId, userId, emoji)
                .orElseGet(() -> commentReactionRepository.save(CommentReaction.builder()
                        .commentId(commentId)
                        .userId(userId)
                        .emoji(emoji)
                        .build()));
    }

    @Transactional
    public void removeReaction(Long commentId, Long userId, String emoji) {
        CommentReaction reaction = commentReactionRepository
                .findByCommentIdAndUserIdAndEmoji(commentId, userId, emoji)
                .orElseThrow(() -> new ApiRequestException(REACTION_NOT_FOUND, HttpStatus.NOT_FOUND));
        commentReactionRepository.delete(reaction);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getReactionCounts(Long commentId) {
        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : commentReactionRepository.countReactionsByCommentId(commentId)) {
            counts.put((String) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Transactional(readOnly = true)
    public List<String> getUserReactions(Long commentId, Long userId) {
        return commentReactionRepository.findUserReactionsByCommentId(commentId, userId);
    }

    private CommentResponse toResponse(Comment comment, BaseUserResponse author) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setContent(comment.getContent());
        response.setCreatedAt(comment.getCreatedAt());
        response.setUser(author);
        return response;
    }
}
