package com.strava.backend.graphql;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.Comment;
import com.strava.backend.dto.activity.ActivityRequest;
import com.strava.backend.dto.activity.ActivityStatsResponse;
import com.strava.backend.dto.activity.WeeklyStatsResponse;
import com.strava.backend.service.ActivityService;
import com.strava.backend.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ActivityGraphQLController {

    private static final Long DEFAULT_USER_ID = 1L;

    private final ActivityService activityService;
    private final CommentService commentService;

    @QueryMapping
    public Activity activity(@Argument Long id) {
        return activityService.getActivityById(id);
    }

    @QueryMapping
    public List<Activity> activities(
            @Argument Long userId,
            @Argument Integer page,
            @Argument Integer size) {
        Long targetUserId = userId != null ? userId : DEFAULT_USER_ID;
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
        return activityService.getFeedActivities(DEFAULT_USER_ID, PageRequest.of(pageNum, pageSize)).getContent();
    }

    @QueryMapping
    public ActivityStatsResponse activityStats(@Argument Long userId) {
        return activityService.getMonthlyStats(userId);
    }

    @QueryMapping
    public WeeklyStatsResponse weeklyStats(@Argument Long userId) {
        return activityService.getWeeklyStats(userId);
    }

    @MutationMapping
    public Activity createActivity(@Argument("input") ActivityRequest input) {
        return activityService.createActivity(DEFAULT_USER_ID, input);
    }

    @MutationMapping
    public Activity updateActivity(@Argument Long id, @Argument("input") ActivityRequest input) {
        return activityService.updateActivity(id, DEFAULT_USER_ID, input);
    }

    @MutationMapping
    public Boolean deleteActivity(@Argument Long id) {
        activityService.deleteActivity(id, DEFAULT_USER_ID);
        return true;
    }

    @MutationMapping
    public Activity likeActivity(@Argument Long id) {
        activityService.likeActivity(id, DEFAULT_USER_ID);
        return activityService.getActivityById(id);
    }

    @MutationMapping
    public Activity unlikeActivity(@Argument Long id) {
        activityService.unlikeActivity(id, DEFAULT_USER_ID);
        return activityService.getActivityById(id);
    }

    @MutationMapping
    public Comment addComment(@Argument Long activityId, @Argument String content) {
        return commentService.createComment(activityId, DEFAULT_USER_ID, content);
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
        return activityService.isLikedByUser(activity.getId(), DEFAULT_USER_ID);
    }

    @SchemaMapping(typeName = "Activity", field = "comments")
    public List<Comment> comments(Activity activity) {
        return commentService.getCommentsByActivityId(activity.getId());
    }
}
