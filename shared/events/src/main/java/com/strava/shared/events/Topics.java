package com.strava.shared.events;

public final class Topics {

    private Topics() {}

    public static final String USER_REGISTERED = "stride.user.registered";
    public static final String USER_UPDATED = "stride.user.updated";
    public static final String USER_DELETED = "stride.user.deleted";
    public static final String USER_FOLLOWED = "stride.user.followed";
    public static final String USER_UNFOLLOWED = "stride.user.unfollowed";

    public static final String ACTIVITY_CREATED = "stride.activity.created";
    public static final String ACTIVITY_UPDATED = "stride.activity.updated";
    public static final String ACTIVITY_DELETED = "stride.activity.deleted";
    public static final String ACTIVITY_VISIBILITY_CHANGED = "stride.activity.visibility-changed";
    public static final String ACTIVITY_LIKED = "stride.activity.liked";

    public static final String PERSONAL_RECORD_ACHIEVED = "stride.pr.achieved";

    public static final String COMMENT_ADDED = "stride.comment.added";
    public static final String REACTION_ADDED = "stride.reaction.added";

    public static final String CHALLENGE_COMPLETED = "stride.challenge.completed";
    public static final String GOAL_ACHIEVED = "stride.goal.achieved";

    public static final String NOTIFICATION_REQUESTED = "stride.notification.requested";
}
