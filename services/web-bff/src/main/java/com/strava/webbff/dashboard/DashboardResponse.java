package com.strava.webbff.dashboard;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Mirrors the monolith's {@code DashboardResponse} field-for-field (same names,
 * same order, nulls serialized like main's Lombok DTO). Section payloads are
 * passed through verbatim as JSON trees from the owning services — each of
 * which already serves the monolith's exact DTO shape — so the composed
 * response stays byte-compatible without duplicating six DTO hierarchies here.
 */
public record DashboardResponse(
        JsonNode user,
        JsonNode stats,
        JsonNode weeklyStats,
        List<JsonNode> recentActivities,
        JsonNode weeklyGoal,
        List<JsonNode> upcomingEvents
) {
}
