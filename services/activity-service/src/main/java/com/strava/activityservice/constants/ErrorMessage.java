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

    // Import errors
    public static final String IMPORT_FILE_EMPTY = "Import file is empty";
    public static final String IMPORT_FILE_TOO_LARGE = "Import file must be less than 10MB";
    public static final String UNSUPPORTED_FILE_FORMAT = "Unsupported file format. Supported: GPX, TCX, FIT";
    public static final String IMPORT_PARSE_ERROR = "Failed to parse activity file";
    public static final String INVALID_SAMPLE_FILENAME = "Invalid sample filename";
    public static final String SAMPLE_FILE_NOT_FOUND = "Sample file not found";

    // Gear errors
    public static final String GEAR_NOT_FOUND = "Gear not found";
    public static final String GEAR_ACCESS_DENIED = "You don't have permission to access this gear";
    public static final String GEAR_ACTIVITY_TYPE_MISMATCH = "This gear cannot be used for the selected activity type";
    public static final String GEAR_ALREADY_RETIRED = "This gear is already retired";

    // GPS stream errors
    public static final String STREAMS_WITHOUT_ROUTE = "Point timestamps and elevation profiles require a route polyline with at least two points";
    public static final String MISALIGNED_POINT_TIMESTAMPS = "pointTimestamps must be a JSON array with exactly one non-decreasing epoch value per route point";
    public static final String MISALIGNED_ELEVATION_PROFILE = "elevationProfile must be a JSON array with exactly one value per route point";

    private ErrorMessage() {
    }
}
