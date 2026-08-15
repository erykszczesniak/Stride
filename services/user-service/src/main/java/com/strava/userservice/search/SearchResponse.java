package com.strava.userservice.search;

import com.strava.userservice.user.BaseUserResponse;

import java.util.List;

/**
 * Global search envelope, mirrored from the monolith's {@code SearchResponse}. Only the
 * {@code users} slice is produced by this service; the other entity types are owned by
 * other services and stay empty until global search composition is wired up (the web
 * app only ever calls the users search).
 */
public record SearchResponse(
        List<BaseUserResponse> users,
        List<Object> activities,
        List<Object> clubs,
        List<Object> challenges,
        List<Object> events
) {
    public static SearchResponse ofUsers(List<BaseUserResponse> users) {
        return new SearchResponse(users, List.of(), List.of(), List.of(), List.of());
    }
}
