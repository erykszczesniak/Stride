package com.strava.userservice.common;

/**
 * Error message constants mirrored 1:1 from the monolith's {@code ErrorMessage} class —
 * the frontend surfaces these verbatim via {@code err.response.data.message}.
 */
public final class ErrorMessages {

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

    // OAuth errors
    public static final String INVALID_GOOGLE_TOKEN = "Invalid Google ID token";
    public static final String GOOGLE_AUTH_ERROR = "Google authentication failed";
    public static final String GOOGLE_AUTH_EMAIL_MISMATCH = "Email from Google account is already registered with a different auth provider";
    public static final String STRAVA_AUTH_ERROR = "Strava authentication failed";
    public static final String STRAVA_PLACEHOLDER_EMAIL_CONFLICT = "Could not sign in with Strava — the account placeholder already exists for a different user";

    // JWT errors
    public static final String INVALID_JWT_TOKEN = "Invalid JWT token";

    private ErrorMessages() {
    }
}
