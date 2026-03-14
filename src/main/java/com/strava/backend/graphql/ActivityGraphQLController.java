package com.strava.backend.graphql;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.Comment;
import com.strava.backend.dto.activity.ActivityRequest;
import com.strava.backend.dto.activity.ActivityStatsResponse;
import com.strava.backend.dto.activity.WeeklyStatsResponse;
import com.strava.backend.security.UserPrincipal;
import com.strava.backend.service.ActivityService;
import com.strava.backend.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ActivityGraphQLController {

    private final ActivityService activityService;
    private final CommentService commentService;

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) authentication.getPrincipal()).getId();
        }
        return null;
    }

    @QueryMapping
    public Activity activity(@Argument Long id) {
        return activityService.getActivityById(id);
    }

    @QueryMapping
    public List<Activity> activities(
            @Argument Long userId,
            @Argument Integer page,
            @Argument Integer size) {
        Long targetUserId = userId != null ? userId : getAuthenticatedUserId();
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 10;
        return activityService.getActivitiesByUserId(targetUserId, PageRequest.of(pageNum, pageSize)).getContent();
    }

    @QueryMapping
    public List<Activity> feedActivities(
            @Argument Integer page,
            @Argument Integer size) {
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 10;
        return activityService.getFeedActivities(getAuthenticatedUserId(), PageRequest.of(pageNum, pageSize)).getContent();
    }

    @QueryMapping
    public ActivityStatsResponse activityStats(@Argument Long userId) {
        Long authenticatedUserId = getAuthenticatedUserId();
        Long targetUserId = userId != null ? userId : authenticatedUserId;
        if (!targetUserId.equals(authenticatedUserId)) {
            throw new org.springframework.security.access.AccessDeniedException("Not authorized to view stats for another user");
        }
        return activityService.getMonthlyStats(targetUserId);
    }

    @QueryMapping
    public WeeklyStatsResponse weeklyStats(@Argument Long userId) {
        Long authenticatedUserId = getAuthenticatedUserId();
        Long targetUserId = userId != null ? userId : authenticatedUserId;
        if (!targetUserId.equals(authenticatedUserId)) {
            throw new org.springframework.security.access.AccessDeniedException("Not authorized to view stats for another user");
        }
        return activityService.getWeeklyStats(targetUserId);
    }

    @MutationMapping
    public Activity createActivity(@Argument("input") ActivityRequest input) {
        return activityService.createActivity(getAuthenticatedUserId(), input);
    }

    @MutationMapping
    public Activity updateActivity(@Argument Long id, @Argument("input") ActivityRequest input) {
        return activityService.updateActivity(id, getAuthenticatedUserId(), input);
    }

    @MutationMapping
    public Boolean deleteActivity(@Argument Long id) {
        activityService.deleteActivity(id, getAuthenticatedUserId());
        return true;
    }

    @MutationMapping
    public Activity likeActivity(@Argument Long id) {
        activityService.likeActivity(id, getAuthenticatedUserId());
        return activityService.getActivityById(id);
    }

    @MutationMapping
    public Activity unlikeActivity(@Argument Long id) {
        activityService.unlikeActivity(id, getAuthenticatedUserId());
        return activityService.getActivityById(id);
    }

    @MutationMapping
    public Comment addComment(@Argument Long activityId, @Argument String content) {
        return commentService.createComment(activityId, getAuthenticatedUserId(), content);
    }

    // Schema mappings for computed fields
    @SchemaMapping(typeName = "Activity", field = "likesCount")
    public int likesCount(Activity activity) {
        return activity.getLikesCount();
    }

    @SchemaMapping(typeName = "Activity", field = "commentsCount")
    public int commentsCount(Activity activity) {
        return activity.getCommentsCount();
    }

    @SchemaMapping(typeName = "Activity", field = "likedByCurrentUser")
    public boolean likedByCurrentUser(Activity activity) {
        return activityService.isLikedByUser(activity.getId(), getAuthenticatedUserId());
    }

    @SchemaMapping(typeName = "Activity", field = "comments")
    public List<Comment> comments(Activity activity) {
        return commentService.getCommentsByActivityId(activity.getId());
    }
}
