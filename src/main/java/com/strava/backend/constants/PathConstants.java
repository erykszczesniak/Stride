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
    public static final String GOOGLE = "/google";
    // Strava login flow — kept separate from the integration flow under /integrations/strava
    // so the callback can be permitAll without opening the import endpoints up.
    public static final String API_V1_AUTH_STRAVA = API_V1_AUTH + "/strava";

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
    public static final String PASSWORD = "/password";

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
    public static final String REACTIONS = "/reactions";
    public static final String USER = "/user";
    public static final String TARGET_USER_ID = "/{targetUserId}";
    public static final String BY_TYPE = "/by-type";
    public static final String PUBLIC = "/public";
    public static final String PHOTOS = "/photos";
    public static final String PHOTO_INDEX = "/{photoIndex}";

    // Common
    public static final String MY = "/my";
    public static final String ALL = "/all";

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

    // AI
    public static final String AI = "/ai";
    public static final String API_V1_AI = API_V1 + AI;
    public static final String GENERATE_GOAL = "/generate-goal";
    public static final String GENERATE_PLAN = "/generate-plan";
    public static final String CHAT = "/chat";

    // Training Plans
    public static final String TRAINING_PLANS = "/training-plans";
    public static final String API_V1_TRAINING_PLANS = API_V1 + TRAINING_PLANS;
    public static final String TEMPLATES = "/templates";
    public static final String TEMPLATE_ID = "/{id}";

    // Notifications
    public static final String NOTIFICATIONS = "/notifications";
    public static final String API_V1_NOTIFICATIONS = API_V1 + NOTIFICATIONS;
    public static final String NOTIFICATION_ID = "/{notificationId}";
    public static final String READ = "/read";
    public static final String READ_ALL = "/read-all";
    public static final String UNREAD_COUNT = "/unread-count";
    public static final String STREAM = "/stream";

    // Import
    public static final String IMPORT = "/import";
    public static final String API_V1_IMPORT = API_V1 + IMPORT;
    public static final String SAMPLES = "/samples";

    // Personal Records
    public static final String PERSONAL_RECORDS = "/personal-records";
    public static final String API_V1_PERSONAL_RECORDS = API_V1 + PERSONAL_RECORDS;
    public static final String ACTIVITY_TYPE = "/activity-type";
    public static final String RECALCULATE = "/recalculate";

    // Gear
    public static final String GEAR = "/gear";
    public static final String API_V1_GEAR = API_V1 + GEAR;
    public static final String GEAR_ID = "/{gearId}";
    public static final String RETIRE = "/retire";

    // Export
    public static final String EXPORT = "/export";
    public static final String API_V1_EXPORT = API_V1 + EXPORT;
    public static final String JSON = "/json";
    public static final String CSV = "/csv";
    public static final String TXT = "/txt";
    public static final String GPX = "/gpx";

    // Integrations (Strava, etc.)
    public static final String INTEGRATIONS = "/integrations";
    public static final String STRAVA = "/strava";
    public static final String API_V1_INTEGRATIONS_STRAVA = API_V1 + INTEGRATIONS + STRAVA;
    public static final String AUTHORIZE = "/authorize";
    public static final String CALLBACK = "/callback";
    public static final String STATUS = "/status";
    public static final String DISCONNECT = "/disconnect";

    // Pipeline facade (read-only, personal-token auth)
    public static final String PIPELINE_V1 = "/pipeline/v1";
    public static final String API_V1_PIPELINE_TOKENS = API_V1 + "/pipeline/tokens";

    private PathConstants() {
    }
}
