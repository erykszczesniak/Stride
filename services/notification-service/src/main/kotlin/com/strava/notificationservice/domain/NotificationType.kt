package com.strava.notificationservice.domain

/**
 * Mirrors the monolith's com.strava.backend.enums.NotificationType 1:1 —
 * the frontend switches on these exact values.
 */
enum class NotificationType {
    LIKE,
    COMMENT,
    FOLLOW,
    ACHIEVEMENT,
    GOAL_COMPLETED,
    SYSTEM
}
