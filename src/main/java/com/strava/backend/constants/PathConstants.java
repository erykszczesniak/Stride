package com.strava.backend.constants;

public final class PathConstants {

    public static final String API_V1 = "/api/v1";

    // Auth
    public static final String AUTH = "/auth";
    public static final String API_V1_AUTH = API_V1 + AUTH;
    public static final String LOGIN = "/login";
    public static final String REGISTER = "/register";
    public static final String REFRESH = "/refresh";
    public static final String LOGOUT = "/logout";
    public static final String FORGOT_PASSWORD = "/forgot-password";
    public static final String RESET_PASSWORD = "/reset-password";

    // Users
    public static final String USERS = "/users";
    public static final String API_V1_USERS = API_V1 + USERS;
    public static final String USER_ID = "/{userId}";
    public static final String ME = "/me";
    public static final String PROFILE = "/profile";
    public static final String AVATAR = "/avatar";
    public static final String FOLLOWERS = "/followers";
    public static final String FOLLOWING = "/following";
    public static final String FOLLOW = "/follow";
    public static final String UNFOLLOW = "/unfollow";
    public static final String SUGGESTED = "/suggested";

    // Activities
    public static final String ACTIVITIES = "/activities";
    public static final String API_V1_ACTIVITIES = API_V1 + ACTIVITIES;
    public static final String ACTIVITY_ID = "/{activityId}";
    public static final String FEED = "/feed";
    public static final String STATS = "/stats";
    public static final String WEEKLY = "/weekly";
    public static final String MONTHLY = "/monthly";
    public static final String YEARLY = "/yearly";
    public static final String LIKE = "/like";
    public static final String UNLIKE = "/unlike";
    public static final String COMMENTS = "/comments";
    public static final String COMMENT_ID = "/{commentId}";

    // Clubs
    public static final String CLUBS = "/clubs";
    public static final String API_V1_CLUBS = API_V1 + CLUBS;
    public static final String CLUB_ID = "/{clubId}";
    public static final String JOIN = "/join";
    public static final String LEAVE = "/leave";
    public static final String MEMBERS = "/members";

    // Challenges
    public static final String CHALLENGES = "/challenges";
    public static final String API_V1_CHALLENGES = API_V1 + CHALLENGES;
    public static final String CHALLENGE_ID = "/{challengeId}";
    public static final String LEADERBOARD = "/leaderboard";
    public static final String PARTICIPATE = "/participate";

    // Events
    public static final String EVENTS = "/events";
    public static final String API_V1_EVENTS = API_V1 + EVENTS;
    public static final String EVENT_ID = "/{eventId}";
    public static final String UPCOMING = "/upcoming";
    public static final String REGISTER_EVENT = "/register";

    // Goals
    public static final String GOALS = "/goals";
    public static final String API_V1_GOALS = API_V1 + GOALS;
    public static final String GOAL_ID = "/{goalId}";
    public static final String PROGRESS = "/progress";

    // Dashboard
    public static final String DASHBOARD = "/dashboard";
    public static final String API_V1_DASHBOARD = API_V1 + DASHBOARD;

    // Search
    public static final String SEARCH = "/search";
    public static final String API_V1_SEARCH = API_V1 + SEARCH;

    // Admin
    public static final String ADMIN = "/admin";
    public static final String API_V1_ADMIN = API_V1 + ADMIN;

    // GraphQL
    public static final String GRAPHQL = "/graphql";

    private PathConstants() {
    }
}
