package com.strava.activityservice.constants;

/**
 * Error texts mirrored 1:1 from main's ErrorMessage (activity/PR subset) —
 * the frontend surfaces these verbatim via err.response.data.message.
 */
public final class ErrorMessage {

    // Activity errors
    public static final String ACTIVITY_NOT_FOUND = "Activity not found";
    public static final String ACTIVITY_ACCESS_DENIED = "You don't have permission to access this activity";
    public static final String EMPTY_ACTIVITY_TITLE = "Activity title cannot be empty";
    public static final String INVALID_DISTANCE = "Distance must be greater than 0";
    public static final String INVALID_DURATION = "Duration must be greater than 0";
    public static final String PHOTO_LIMIT_EXCEEDED = "Maximum 5 photos per activity";
    public static final String INVALID_PHOTO_TYPE = "Only JPG, PNG, and WebP images are allowed";
    public static final String PHOTO_TOO_LARGE = "Photo must be less than 5MB";
    public static final String INVALID_PHOTO_INDEX = "Invalid photo index";

    // GPS stream errors
    public static final String STREAMS_WITHOUT_ROUTE = "Point timestamps and elevation profiles require a route polyline with at least two points";
    public static final String MISALIGNED_POINT_TIMESTAMPS = "pointTimestamps must be a JSON array with exactly one non-decreasing epoch value per route point";
    public static final String MISALIGNED_ELEVATION_PROFILE = "elevationProfile must be a JSON array with exactly one value per route point";

    private ErrorMessage() {
    }
}
