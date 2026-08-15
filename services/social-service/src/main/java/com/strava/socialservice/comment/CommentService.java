package com.strava.socialservice.comment;

import com.strava.socialservice.domain.Comment;
import com.strava.socialservice.domain.CommentRepository;
import com.strava.socialservice.domain.FeedItemRepository;
import com.strava.socialservice.domain.UserCache;
import com.strava.socialservice.domain.UserCacheRepository;
import com.strava.socialservice.event.SocialEventPublisher;
import com.strava.socialservice.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository comments;
    private final FeedItemRepository feed;
    private final UserCacheRepository users;
    private final SocialEventPublisher events;

    @Transactional
    public CommentDto create(Long activityId, Long userId, CreateCommentRequest req) {
        Comment c = Comment.builder()
                .activityId(activityId)
                .userId(userId)
                .content(req.content())
                .build();
        comments.save(c);
        Long ownerId = feed.findByActivityId(activityId)
                .map(f -> f.getUserId())
                .orElse(null);
        String authorName = users.findById(userId)
                .map(UserCache::getDisplayName)
                .orElse(null);
        events.publishCommentAdded(c, ownerId, authorName);
        return CommentDto.from(c);
    }

    @Transactional(readOnly = true)
    public Page<CommentDto> list(Long activityId, Pageable pageable) {
        return comments.findAllByActivityIdOrderByCreatedAtDesc(activityId, pageable).map(CommentDto::from);
    }

    @Transactional
    public void delete(Long commentId, Long userId) {
        Comment c = comments.findByIdAndUserId(commentId, userId)
                .orElseThrow(() -> new NotFoundException("Comment " + commentId + " not found"));
        comments.delete(c);
    }

    @Transactional(readOnly = true)
    public long countForActivity(Long activityId) {
        return comments.countByActivityId(activityId);
    }
}
