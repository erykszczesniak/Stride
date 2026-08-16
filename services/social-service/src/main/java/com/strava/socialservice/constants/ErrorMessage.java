package com.strava.socialservice.constants;

/** The subset of main's ErrorMessage constants this service surfaces — texts must match main exactly. */
public final class ErrorMessage {

    public static final String ACTIVITY_NOT_FOUND = "Activity not found";
    public static final String COMMENT_NOT_FOUND = "Comment not found";
    public static final String REACTION_NOT_FOUND = "Reaction not found";
    public static final String ACCESS_DENIED = "Access denied";
    public static final String EMPTY_COMMENT = "Comment cannot be empty";
    public static final String ACTIVITY_ALREADY_LIKED = "Activity already liked";
    public static final String ACTIVITY_NOT_LIKED = "Activity not liked";

    private ErrorMessage() {
    }
}
