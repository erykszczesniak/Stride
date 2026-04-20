package com.strava.backend.constants;

public final class ErrorMessage {

    // User errors
    public static final String USER_NOT_FOUND = "User not found";
    public static final String USER_ALREADY_EXISTS = "User with this email already exists";
    public static final String INVALID_CREDENTIALS = "Invalid email or password";
    public static final String EMAIL_NOT_FOUND = "Email not found";
    public static final String PASSWORDS_DO_NOT_MATCH = "Passwords do not match";
    public static final String INVALID_PASSWORD_CODE = "Invalid password reset code";
    public static final String PASSWORD_CODE_EXPIRED = "Password reset code has expired";
    public static final String INCORRECT_PASSWORD = "Current password is incorrect";
    public static final String PASSWORD_CHANGE_NOT_ALLOWED = "Password change is not available for accounts using Google sign-in";

    // Validation errors
    public static final String EMPTY_FIRST_NAME = "First name cannot be empty";
    public static final String EMPTY_LAST_NAME = "Last name cannot be empty";
    public static final String EMPTY_EMAIL = "Email cannot be empty";
    public static final String INVALID_EMAIL = "Please enter a valid email address";
    public static final String EMPTY_PASSWORD = "Password cannot be empty";
    public static final String PASSWORD_MIN_LENGTH = "Password must be at least 8 characters";
    public static final String PASSWORD_MAX_LENGTH = "Password must be less than 50 characters";

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

    // Club errors
    public static final String CLUB_NOT_FOUND = "Club not found";
    public static final String ALREADY_CLUB_MEMBER = "You are already a member of this club";
    public static final String NOT_CLUB_MEMBER = "You are not a member of this club";
    public static final String CLUB_ACCESS_DENIED = "You don't have permission to manage this club";

    // Challenge errors
    public static final String CHALLENGE_NOT_FOUND = "Challenge not found";
    public static final String ALREADY_PARTICIPATING = "You are already participating in this challenge";
    public static final String CHALLENGE_ENDED = "This challenge has already ended";
    public static final String CHALLENGE_NOT_STARTED = "This challenge has not started yet";

    // Event errors
    public static final String EVENT_NOT_FOUND = "Event not found";
    public static final String ALREADY_REGISTERED = "You are already registered for this event";
    public static final String EVENT_FULL = "This event is full";
    public static final String REGISTRATION_CLOSED = "Registration for this event is closed";

    // Goal errors
    public static final String GOAL_NOT_FOUND = "Goal not found";
    public static final String GOAL_ACCESS_DENIED = "You don't have permission to access this goal";

    // Comment errors
    public static final String COMMENT_NOT_FOUND = "Comment not found";
    public static final String EMPTY_COMMENT = "Comment cannot be empty";

    // OAuth errors
    public static final String INVALID_GOOGLE_TOKEN = "Invalid Google ID token";
    public static final String GOOGLE_AUTH_ERROR = "Google authentication failed";
    public static final String GOOGLE_AUTH_EMAIL_MISMATCH = "Email from Google account is already registered with a different auth provider";

    // JWT errors
    public static final String INVALID_JWT_TOKEN = "Invalid JWT token";
    public static final String JWT_TOKEN_EXPIRED = "JWT token has expired";
    public static final String JWT_TOKEN_UNSUPPORTED = "JWT token is unsupported";

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

    // General errors
    public static final String ACCESS_DENIED = "Access denied";
    public static final String INTERNAL_SERVER_ERROR = "Internal server error";

    private ErrorMessage() {
    }
}
